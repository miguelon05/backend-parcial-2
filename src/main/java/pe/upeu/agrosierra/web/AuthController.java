package pe.upeu.agrosierra.web;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pe.upeu.agrosierra.servicio.AuthService;
import pe.upeu.agrosierra.web.dto.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service=service; }
    @PostMapping("/login") public SesionResponse ingresar(@Valid @RequestBody LoginRequest r) { return service.ingresar(r); }
}
