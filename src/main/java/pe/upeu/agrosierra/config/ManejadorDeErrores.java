package pe.upeu.agrosierra.config;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.servlet.http.HttpServletRequest;
import pe.upeu.agrosierra.web.dto.ErrorResponse;
import java.util.*;
import org.slf4j.*;
@RestControllerAdvice
public class ManejadorDeErrores {
    private static final Logger log=LoggerFactory.getLogger(ManejadorDeErrores.class);
    private final Errores errores;
    public ManejadorDeErrores(Errores errores) { this.errores=errores; }
    private ResponseEntity<ErrorResponse> respuesta(HttpStatus s,String m,Map<String,String> c,HttpServletRequest r) {
        return ResponseEntity.status(s).body(errores.cuerpo(s,m,r.getRequestURI(),c));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> negocio(ApiException e,HttpServletRequest r) { return respuesta(e.status,e.getMessage(),e.campos,r); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e,HttpServletRequest r) {
        var campos=new TreeMap<String,String>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST,"Datos invalidos",campos,r);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> parametro(MethodArgumentTypeMismatchException e,HttpServletRequest r) {
        return respuesta(HttpStatus.BAD_REQUEST,"Parametro invalido",Map.of(e.getName(),"Valor incompatible"),r);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> json(HttpMessageNotReadableException e,HttpServletRequest r) {
        String campo="cuerpo";
        Throwable causa=e;
        while(causa!=null) {
            if(causa instanceof tools.jackson.databind.DatabindException d && !d.getPath().isEmpty()) {
                var nombre=d.getPath().getLast().getPropertyName(); if(nombre!=null) campo=nombre;
                break;
            }
            causa=causa.getCause();
        }
        return respuesta(HttpStatus.BAD_REQUEST,"JSON invalido: revise tipos, fecha ISO y tipo de labor",Map.of(campo,"Valor invalido"),r);
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> prohibido(AccessDeniedException e,HttpServletRequest r) { return respuesta(HttpStatus.FORBIDDEN,"No tiene el privilegio necesario",Map.of(),r); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException e,HttpServletRequest r) { return respuesta(HttpStatus.CONFLICT,"El registro entra en conflicto con datos existentes",Map.of(),r); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> inexistente(Exception e,HttpServletRequest r) { return respuesta(HttpStatus.NOT_FOUND,"Ruta no encontrada",Map.of(),r); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> metodo(Exception e,HttpServletRequest r) { return respuesta(HttpStatus.METHOD_NOT_ALLOWED,"Metodo no permitido",Map.of(),r); }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> faltante(MissingServletRequestParameterException e,HttpServletRequest r) { return respuesta(HttpStatus.BAD_REQUEST,"Falta un parametro",Map.of(e.getParameterName(),"Es obligatorio"),r); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> inesperado(Exception e,HttpServletRequest r) {
        log.error("Error inesperado en {}",r.getRequestURI(),e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,"No se pudo completar la operacion",Map.of(),r);
    }
}
