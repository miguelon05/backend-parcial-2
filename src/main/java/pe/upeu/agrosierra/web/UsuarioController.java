package pe.upeu.agrosierra.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import pe.upeu.agrosierra.seguridad.*;
import pe.upeu.agrosierra.servicio.*;
import pe.upeu.agrosierra.web.dto.*;
@RestController @RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final AuthService auth; private final UsuarioService usuarios;
    public UsuarioController(AuthService auth,UsuarioService usuarios) { this.auth=auth;this.usuarios=usuarios; }
    @GetMapping("/me") public UsuarioResponse me(@AuthenticationPrincipal UsuarioActual actual) { return auth.perfil(actual.id()); }
    @GetMapping public PaginaResponse<UsuarioResponse> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanio) { return usuarios.listar(pagina,tamanio); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest r) { return usuarios.crear(r); }
    @PutMapping("/{id}/rol") public UsuarioResponse rol(@PathVariable Long id,@Valid @RequestBody AsignarRolRequest r) { return usuarios.asignar(id,r); }
}
