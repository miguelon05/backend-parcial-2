package pe.upeu.agrosierra.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import pe.upeu.agrosierra.seguridad.Rol;
import pe.upeu.agrosierra.servicio.UsuarioService;
import pe.upeu.agrosierra.web.dto.*;
@RestController @RequestMapping("/api/v1/roles")
public class RolController {
    private final UsuarioService usuarios;
    public RolController(UsuarioService usuarios) { this.usuarios=usuarios; }
    @GetMapping public PaginaResponse<Rol> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanio) { return usuarios.roles(pagina,tamanio); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Rol crear(@Valid @RequestBody RolRequest r) { return usuarios.crearRol(r); }
}
