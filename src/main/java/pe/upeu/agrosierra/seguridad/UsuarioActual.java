package pe.upeu.agrosierra.seguridad;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
public record UsuarioActual(Long id, String usuario, boolean editaTodas, List<GrantedAuthority> authorities) {
    public static UsuarioActual de(Usuario u) {
        List<GrantedAuthority> permisos=u.getRol().getPrivilegios().stream()
            .map(p -> (GrantedAuthority)new SimpleGrantedAuthority(p.name())).toList();
        return new UsuarioActual(u.getId(),u.getUsuario(),u.getRol().getEditaTodas(),permisos);
    }
}
