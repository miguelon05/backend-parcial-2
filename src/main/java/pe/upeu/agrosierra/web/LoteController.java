package pe.upeu.agrosierra.web;

import org.springframework.web.bind.annotation.*;
import pe.upeu.agrosierra.servicio.*;
import pe.upeu.agrosierra.web.dto.*;
@RestController @RequestMapping("/api/v1/lotes")
public class LoteController {
    private final LoteService lotes; private final LaborService labores;
    public LoteController(LoteService lotes,LaborService labores) { this.lotes=lotes;this.labores=labores; }
    @GetMapping public PaginaResponse<LoteResponse> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanio,
        @RequestParam(required=false) String valle,@RequestParam(required=false) Boolean campaniaCerrada,@RequestParam(required=false) String busqueda) {
        return lotes.listar(pagina,tamanio,valle,campaniaCerrada,busqueda);
    }
    @GetMapping("/{id}") public LoteResponse buscar(@PathVariable Long id) { return lotes.buscar(id); }
    @GetMapping("/{id}/labores") public PaginaResponse<LaborResponse> labores(@PathVariable Long id,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanio) { return labores.listar(id,pagina,tamanio); }
    @PostMapping("/{id}/cerrar-campania") public LoteResponse cerrar(@PathVariable Long id) { return lotes.cerrar(id); }
}
