package pe.upeu.agrosierra.servicio.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import pe.upeu.agrosierra.config.ApiException;
import pe.upeu.agrosierra.dominio.*;
import pe.upeu.agrosierra.repositorio.*;
import pe.upeu.agrosierra.seguridad.*;
import pe.upeu.agrosierra.servicio.*;
import pe.upeu.agrosierra.web.dto.*;
import pe.upeu.agrosierra.web.mapper.AgroMapper;
@Service @Transactional(readOnly=true)
public class LaborServiceImpl implements LaborService {
    private final LoteRepository lotes; private final LaborRepository labores;
    private final UsuarioRepository usuarios; private final AgroMapper mapper; private final Clock clock;
    public LaborServiceImpl(LoteRepository lotes,LaborRepository labores,UsuarioRepository usuarios,AgroMapper mapper,Clock clock) {
        this.lotes=lotes; this.labores=labores; this.usuarios=usuarios; this.mapper=mapper; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('LABOR_LEER')")
    public PaginaResponse<LaborResponse> listar(Long id,int pagina,int tamanio) {
        if(!lotes.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND,"Lote no encontrado");
        return PaginaResponse.de(labores.findByLoteId(id,Paginacion.de(pagina,tamanio,Sort.by(Sort.Order.desc("fecha"),Sort.Order.desc("creadoEn"),Sort.Order.desc("id")))).map(mapper::labor));
    }
    private Lote bloquear(Long id) { return lotes.bloquear(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Lote no encontrado")); }
    private Labor obtener(Long id) { return labores.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Labor no encontrada")); }
    private Long loteDeLabor(Long id) { return labores.loteDeLabor(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Labor no encontrada")); }
    private void validar(Lote lote,LaborRequest r) {
        if(lote.getCampaniaCerrada()) throw new ApiException(HttpStatus.CONFLICT,"RN-01: no se puede registrar ni corregir labores en una campania cerrada");
        if(!Double.isFinite(r.cantidad())) throw new ApiException(HttpStatus.BAD_REQUEST,"Cantidad invalida",Map.of("cantidad","Debe ser un numero finito"));
        if(r.tipo()==TipoLabor.FUMIGACION && r.cantidad()>lote.getDosisMaximaPorHectarea()*lote.getHectareas())
            throw new ApiException(HttpStatus.BAD_REQUEST,"RN-04: la fumigacion supera la dosis maxima",Map.of("cantidad","Maximo permitido: "+(lote.getDosisMaximaPorHectarea()*lote.getHectareas())));
    }
    private void copiar(Labor l,LaborRequest r) { l.setTipo(r.tipo()); l.setFecha(r.fecha()); l.setCantidad(r.cantidad()); l.setObservacion(r.observacion()); }
    private LaborResponse reintento(Labor l,LaborRequest r) {
        if(!l.getLote().getId().equals(r.loteId()) || l.getTipo()!=r.tipo() || !l.getFecha().equals(r.fecha()) || Double.compare(l.getCantidad(),r.cantidad())!=0 || !Objects.equals(l.getObservacion()==null?"":l.getObservacion(),r.observacion()))
            throw new ApiException(HttpStatus.CONFLICT,"La claveCliente ya corresponde a otra peticion");
        return mapper.labor(l);
    }
    @Transactional @PreAuthorize("hasAuthority('LABOR_CREAR')")
    public LaborResponse crear(LaborRequest r,UsuarioActual actual) {
        Lote lote=bloquear(r.loteId());
        // Bloquear al usuario serializa tambien claves iguales enviadas a lotes distintos.
        Usuario usuario=usuarios.bloquear(actual.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Usuario no disponible"));
        if(r.claveCliente()!=null) {
            var existente=labores.findByResponsableIdAndClaveCliente(actual.id(),r.claveCliente());
            if(existente.isPresent()) return reintento(existente.get(),r);
        }
        validar(lote,r);
        Labor l=new Labor(); l.setLote(lote); l.setResponsable(usuario); l.setCreadoEn(clock.instant()); l.setClaveCliente(r.claveCliente());
        copiar(l,r); return mapper.labor(labores.saveAndFlush(l));
    }
    @Transactional @PreAuthorize("hasAuthority('LABOR_EDITAR')")
    public LaborResponse actualizar(Long id,LaborRequest r,UsuarioActual actual) {
        Long loteId=loteDeLabor(id);
        if(!loteId.equals(r.loteId())) throw new ApiException(HttpStatus.BAD_REQUEST,"No se puede trasladar una labor a otro lote",Map.of("loteId","Debe coincidir con el lote original"));
        Lote lote=bloquear(r.loteId());
        // Cargar la entidad despues del bloqueo permite ver correcciones concurrentes confirmadas.
        Labor l=obtener(id);
        validar(lote,r);
        if(!actual.editaTodas() && (!l.getResponsable().getId().equals(actual.id()) || !clock.instant().isBefore(l.getCreadoEn().plus(Duration.ofHours(24)))))
            throw new ApiException(HttpStatus.FORBIDDEN,"RN-02: solo puede corregir sus labores dentro de las primeras 24 horas");
        copiar(l,r); return mapper.labor(labores.saveAndFlush(l));
    }
    @Transactional @PreAuthorize("hasAuthority('LABOR_ELIMINAR')")
    public void eliminar(Long id) { bloquear(loteDeLabor(id)); labores.delete(obtener(id)); labores.flush(); }
}
