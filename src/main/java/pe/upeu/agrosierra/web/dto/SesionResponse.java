package pe.upeu.agrosierra.web.dto;

import java.util.Set;
public record SesionResponse(String token, String usuario, String rol, Set<String> privilegios, String nombre, long expiraEn, boolean editaTodas) {}
