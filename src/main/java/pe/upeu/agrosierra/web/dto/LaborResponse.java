package pe.upeu.agrosierra.web.dto;

import java.time.*;
import pe.upeu.agrosierra.dominio.TipoLabor;
public record LaborResponse(Long id, Long loteId, TipoLabor tipo, LocalDate fecha, String responsable,
    Double cantidad, String observacion, Instant creadoEn) {}
