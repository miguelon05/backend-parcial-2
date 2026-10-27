package pe.upeu.agrosierra.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="lote") @Getter @Setter
public class Lote {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=30) private String codigo;
    @Column(nullable=false, length=120) private String nombre;
    @Column(nullable=false) private Double hectareas;
    @Column(nullable=false, length=80) private String cultivo;
    @Column(nullable=false, length=80) private String valle;
    @Column(nullable=false) private Boolean campaniaCerrada = false;
    @Column(nullable=false) private Double dosisMaximaPorHectarea;
}
