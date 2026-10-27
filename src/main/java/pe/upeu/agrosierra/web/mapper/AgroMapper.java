package pe.upeu.agrosierra.web.mapper;

import org.springframework.stereotype.Component;
import pe.upeu.agrosierra.dominio.*;
import pe.upeu.agrosierra.seguridad.Usuario;
import pe.upeu.agrosierra.web.dto.*;
import java.util.stream.Collectors;
@Component
public class AgroMapper {
    public LoteResponse lote(Lote l) { return new LoteResponse(l.getId(),l.getCodigo(),l.getNombre(),l.getHectareas(),l.getCultivo(),l.getValle(),l.getCampaniaCerrada(),l.getDosisMaximaPorHectarea()); }
    public LaborResponse labor(Labor l) { return new LaborResponse(l.getId(),l.getLote().getId(),l.getTipo(),l.getFecha(),l.getResponsable().getUsuario(),l.getCantidad(),l.getObservacion()==null?"":l.getObservacion(),l.getCreadoEn()); }
    public UsuarioResponse usuario(Usuario u) { return new UsuarioResponse(u.getId(),u.getUsuario(),u.getRol().getNombre(),u.getRol().getPrivilegios().stream().map(Enum::name).collect(Collectors.toSet()),u.getNombre(),u.getRol().getEditaTodas()); }
}
