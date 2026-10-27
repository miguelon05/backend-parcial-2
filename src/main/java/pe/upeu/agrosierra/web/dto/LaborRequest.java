package pe.upeu.agrosierra.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import pe.upeu.agrosierra.dominio.TipoLabor;
public record LaborRequest(
    @NotNull @Positive Long loteId,
    @NotNull TipoLabor tipo,
    @NotNull LocalDate fecha,
    @NotNull @Positive Double cantidad,
    @NotNull @Size(max=1000) String observacion,
    @Size(max=100) @Pattern(regexp=".*\\S.*", message="La clave no puede estar vacia") String claveCliente) {}
