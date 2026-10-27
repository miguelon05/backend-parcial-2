package pe.upeu.agrosierra.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import pe.upeu.agrosierra.seguridad.Usuario;
@Entity @Table(name="labor", uniqueConstraints=@UniqueConstraint(columnNames={"responsable_id", "clave_cliente"})) @Getter @Setter
public class Labor {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="lote_id") private Lote lote;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private TipoLabor tipo;
    @Column(nullable=false) private LocalDate fecha;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="responsable_id") private Usuario responsable;
    @Column(nullable=false) private Double cantidad;
    @Column(length=1000) private String observacion = "";
    @Column(nullable=false) private Instant creadoEn;
    @Column(name="clave_cliente", length=100) private String claveCliente;
}
