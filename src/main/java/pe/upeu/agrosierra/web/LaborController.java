package pe.upeu.agrosierra.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import pe.upeu.agrosierra.seguridad.UsuarioActual;
import pe.upeu.agrosierra.servicio.LaborService;
import pe.upeu.agrosierra.web.dto.*;
@RestController @RequestMapping("/api/v1/labores")
public class LaborController {
    private final LaborService service;
    public LaborController(LaborService service) { this.service=service; }
    @PostMapping public ResponseEntity<LaborResponse> crear(@Valid @RequestBody LaborRequest r,@AuthenticationPrincipal UsuarioActual actual) {
        LaborResponse l=service.crear(r,actual); return ResponseEntity.created(URI.create("/api/v1/lotes/"+l.loteId()+"/labores")).body(l);
    }
    @PutMapping("/{id}") public LaborResponse actualizar(@PathVariable Long id,@Valid @RequestBody LaborRequest r,@AuthenticationPrincipal UsuarioActual actual) { return service.actualizar(id,r,actual); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { service.eliminar(id);return ResponseEntity.noContent().build(); }
}
