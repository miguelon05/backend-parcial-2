package pe.upeu.agrosierra.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="usuario") @Getter @Setter
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String usuario;
    @Column(nullable=false, length=100) private String contrasena;
    @Column(nullable=false, length=120) private String nombre;
    @ManyToOne(optional=false, fetch=FetchType.EAGER) @JoinColumn(name="rol_nombre") private Rol rol;
    @Column(nullable=false) private Boolean activo = true;
}
