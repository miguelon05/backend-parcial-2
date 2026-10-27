package pe.upeu.agrosierra.web.dto;

public record LoteResponse(Long id, String codigo, String nombre, Double hectareas, String cultivo,
    String valle, Boolean campaniaCerrada, Double dosisMaximaPorHectarea) {}
