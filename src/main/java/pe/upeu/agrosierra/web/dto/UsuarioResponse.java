package pe.upeu.agrosierra.web.dto;

import java.util.Set;
public record UsuarioResponse(Long id, String usuario, String rol, Set<String> privilegios, String nombre, boolean editaTodas) {}
