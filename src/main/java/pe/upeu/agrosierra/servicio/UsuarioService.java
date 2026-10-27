package pe.upeu.agrosierra.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import java.util.*;
import pe.upeu.agrosierra.config.ApiException;
import pe.upeu.agrosierra.seguridad.*;
import pe.upeu.agrosierra.web.dto.*;
import pe.upeu.agrosierra.web.mapper.AgroMapper;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAuthority('USUARIO_GESTIONAR')")
public class UsuarioService {
    private final UsuarioRepository usuarios; private final RolRepository roles; private final PasswordEncoder encoder; private final AgroMapper mapper;
    public UsuarioService(UsuarioRepository usuarios,RolRepository roles,PasswordEncoder encoder,AgroMapper mapper) { this.usuarios=usuarios;this.roles=roles;this.encoder=encoder;this.mapper=mapper; }
    private Rol rol(String nombre) { return roles.findById(nombre).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,"Rol desconocido",Map.of("rol","Seleccione un rol existente"))); }
    public PaginaResponse<UsuarioResponse> listar(int p,int t) { return PaginaResponse.de(usuarios.findAll(Paginacion.de(p,t,Sort.by("id"))).map(mapper::usuario)); }
    @Transactional public UsuarioResponse crear(UsuarioRequest r) {
        if(usuarios.existsByUsuario(r.usuario())) throw new ApiException(HttpStatus.CONFLICT,"El usuario ya existe");
        Usuario u=new Usuario(); u.setUsuario(r.usuario());u.setContrasena(encoder.encode(r.contrasena()));u.setNombre(r.nombre());u.setRol(rol(r.rol()));
        return mapper.usuario(usuarios.saveAndFlush(u));
    }
    @Transactional public UsuarioResponse asignar(Long id,AsignarRolRequest r) {
        Usuario u=usuarios.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Usuario no encontrado"));u.setRol(rol(r.rol()));return mapper.usuario(usuarios.save(u));
    }
    public PaginaResponse<Rol> roles(int p,int t) { return PaginaResponse.de(roles.findAll(Paginacion.de(p,t,Sort.by("nombre")))); }
    @Transactional public Rol crearRol(RolRequest r) {
        if(roles.existsById(r.nombre())) throw new ApiException(HttpStatus.CONFLICT,"El rol ya existe");
        Rol rol=new Rol();rol.setNombre(r.nombre());rol.setEditaTodas(r.editaTodas());rol.setPrivilegios(new HashSet<>(r.privilegios()));return roles.saveAndFlush(rol);
    }
}
