package pe.upeu.agrosierra.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import java.time.*;
import pe.upeu.agrosierra.config.*;
import pe.upeu.agrosierra.seguridad.*;
import pe.upeu.agrosierra.web.dto.*;
import pe.upeu.agrosierra.web.mapper.AgroMapper;
@Service @Transactional(readOnly=true)
public class AuthService {
    private final UsuarioRepository usuarios; private final PasswordEncoder encoder; private final JwtService jwt; private final Clock clock; private final AgroMapper mapper;
    private final String hashInexistente;
    public AuthService(UsuarioRepository usuarios,PasswordEncoder encoder,JwtService jwt,Clock clock,AgroMapper mapper) {
        this.usuarios=usuarios; this.encoder=encoder; this.jwt=jwt; this.clock=clock; this.mapper=mapper;
        this.hashInexistente=encoder.encode(java.util.UUID.randomUUID().toString());
    }
    public SesionResponse ingresar(LoginRequest r) {
        Usuario u=usuarios.findByUsuario(r.usuario()).orElse(null);
        boolean valida=encoder.matches(r.contrasena(),u==null?hashInexistente:u.getContrasena());
        if(u==null || !valida || !u.getActivo()) throw new ApiException(HttpStatus.UNAUTHORIZED,"Usuario o contrasena incorrectos");
        Instant expira=clock.instant().plus(Duration.ofHours(1)); var dto=mapper.usuario(u);
        return new SesionResponse(jwt.crear(u,expira),dto.usuario(),dto.rol(),dto.privilegios(),dto.nombre(),expira.getEpochSecond(),dto.editaTodas());
    }
    public UsuarioResponse perfil(Long id) { return mapper.usuario(usuarios.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,"Usuario no disponible"))); }
}
