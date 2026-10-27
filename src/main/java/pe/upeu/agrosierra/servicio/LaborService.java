package pe.upeu.agrosierra.servicio;

import pe.upeu.agrosierra.web.dto.*;
import pe.upeu.agrosierra.seguridad.UsuarioActual;
public interface LaborService {
    PaginaResponse<LaborResponse> listar(Long loteId,int pagina,int tamanio);
    LaborResponse crear(LaborRequest request,UsuarioActual actual);
    LaborResponse actualizar(Long id,LaborRequest request,UsuarioActual actual);
    void eliminar(Long id);
}
