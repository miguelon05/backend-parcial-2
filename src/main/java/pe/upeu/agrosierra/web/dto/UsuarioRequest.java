package pe.upeu.agrosierra.web.dto;

import jakarta.validation.constraints.*;
public record UsuarioRequest(
    @NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{3,50}", message="Use entre 3 y 50 letras, numeros, puntos o guiones") String usuario,
    @NotBlank @Size(min=8, max=72) String contrasena,
    @NotBlank @Size(max=120) String nombre,
    @NotBlank @Size(max=40) String rol) {}
