package pe.upeu.agrosierra.servicio;

import pe.upeu.agrosierra.web.dto.*;
public interface LoteService {
    PaginaResponse<LoteResponse> listar(int pagina,int tamanio,String valle,Boolean cerrada,String busqueda);
    LoteResponse buscar(Long id);
    LoteResponse cerrar(Long id);
}
