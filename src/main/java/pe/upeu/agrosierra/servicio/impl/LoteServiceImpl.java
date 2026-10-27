package pe.upeu.agrosierra.servicio.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import pe.upeu.agrosierra.config.ApiException;
import pe.upeu.agrosierra.dominio.*;
import pe.upeu.agrosierra.repositorio.*;
import pe.upeu.agrosierra.servicio.*;
import pe.upeu.agrosierra.web.dto.*;
import pe.upeu.agrosierra.web.mapper.AgroMapper;
@Service @Transactional(readOnly=true)
public class LoteServiceImpl implements LoteService {
    private final LoteRepository lotes; private final LaborRepository labores; private final AgroMapper mapper;
    public LoteServiceImpl(LoteRepository lotes,LaborRepository labores,AgroMapper mapper) { this.lotes=lotes; this.labores=labores; this.mapper=mapper; }
    @PreAuthorize("hasAuthority('LOTE_LEER')")
    public PaginaResponse<LoteResponse> listar(int pagina,int tamanio,String valle,Boolean cerrada,String busqueda) {
        Specification<Lote> filtro=(root,q,cb) -> {
            var condiciones=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if(valle!=null && !valle.isBlank()) condiciones.add(cb.equal(cb.lower(root.get("valle")),valle.trim().toLowerCase(java.util.Locale.ROOT)));
            if(cerrada!=null) condiciones.add(cb.equal(root.get("campaniaCerrada"),cerrada));
            if(busqueda!=null && !busqueda.isBlank()) {
                String texto="%"+busqueda.trim().toLowerCase(java.util.Locale.ROOT)+"%";
                condiciones.add(cb.or(cb.like(cb.lower(root.get("nombre")),texto),cb.like(cb.lower(root.get("codigo")),texto)));
            }
            return cb.and(condiciones.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PaginaResponse.de(lotes.findAll(filtro,Paginacion.de(pagina,tamanio,Sort.by("id"))).map(mapper::lote));
    }
    @PreAuthorize("hasAuthority('LOTE_LEER')")
    public LoteResponse buscar(Long id) { return mapper.lote(lotes.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Lote no encontrado"))); }
    @Transactional @PreAuthorize("hasAuthority('CAMPANIA_CERRAR')")
    public LoteResponse cerrar(Long id) {
        // El mismo bloqueo se usa al modificar labores: cierre y registro no pueden intercalarse.
        Lote lote=lotes.bloquear(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Lote no encontrado"));
        if(lote.getCampaniaCerrada()) throw new ApiException(HttpStatus.CONFLICT,"La campania ya esta cerrada");
        if(!labores.existsByLoteIdAndTipo(id,TipoLabor.COSECHA)) throw new ApiException(HttpStatus.CONFLICT,"RN-03: registre al menos una cosecha antes de cerrar la campania");
        lote.setCampaniaCerrada(true); return mapper.lote(lotes.save(lote));
    }
}
