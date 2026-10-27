package pe.upeu.agrosierra.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.*;
@Entity @Table(name="rol") @Getter @Setter
public class Rol {
    @Id @Column(length=40) private String nombre;
    @Column(nullable=false) private Boolean editaTodas = false;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="rol_privilegio", joinColumns=@JoinColumn(name="rol_nombre"))
    @Enumerated(EnumType.STRING) @Column(name="privilegio", length=40)
    private Set<Privilegio> privilegios = new HashSet<>();
}
