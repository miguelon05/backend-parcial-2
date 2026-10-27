package pe.upeu.agrosierra.repositorio;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import pe.upeu.agrosierra.dominio.Lote;
public interface LoteRepository extends JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from Lote l where l.id=:id")
    Optional<Lote> bloquear(@Param("id") Long id);
}
