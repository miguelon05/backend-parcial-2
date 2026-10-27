package pe.upeu.agrosierra.web.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import pe.upeu.agrosierra.seguridad.Privilegio;
public record RolRequest(@NotBlank @Pattern(regexp="[A-Z_]{3,40}") String nombre,
    @NotNull Set<@NotNull Privilegio> privilegios, boolean editaTodas) {}
