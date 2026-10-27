package pe.upeu.agrosierra.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.Optional;
import pe.upeu.agrosierra.dominio.*;
public interface LaborRepository extends JpaRepository<Labor, Long> {
    @org.springframework.data.jpa.repository.Query("select l.lote.id from Labor l where l.id=:id")
    Optional<Long> loteDeLabor(@org.springframework.data.repository.query.Param("id") Long id);
    Page<Labor> findByLoteId(Long loteId, Pageable pageable);
    boolean existsByLoteIdAndTipo(Long loteId, TipoLabor tipo);
    Optional<Labor> findByResponsableIdAndClaveCliente(Long usuarioId, String claveCliente);
}
