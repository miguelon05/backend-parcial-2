package pe.upeu.agrosierra.web.dto;

import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank String usuario, @NotBlank String contrasena) {}
