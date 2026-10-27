package pe.upeu.agrosierra.web.dto;

import jakarta.validation.constraints.*;
public record AsignarRolRequest(@NotBlank @Size(max=40) String rol) {}
