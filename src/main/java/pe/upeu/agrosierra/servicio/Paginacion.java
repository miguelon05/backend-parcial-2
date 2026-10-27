package pe.upeu.agrosierra.servicio;

import org.springframework.data.domain.*;
import pe.upeu.agrosierra.config.ApiException;
import org.springframework.http.HttpStatus;
import java.util.Map;
public final class Paginacion {
    private Paginacion() {}
    public static Pageable de(int pagina,int tamanio,Sort sort) {
        if(pagina<0) throw new ApiException(HttpStatus.BAD_REQUEST,"Paginacion invalida",Map.of("pagina","Debe ser mayor o igual a cero"));
        if(tamanio<1 || tamanio>100) throw new ApiException(HttpStatus.BAD_REQUEST,"Paginacion invalida",Map.of("tamanio","Debe estar entre 1 y 100"));
        return PageRequest.of(pagina,tamanio,sort);
    }
}
