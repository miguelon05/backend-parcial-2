package pe.upeu.agrosierra.config;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import io.jsonwebtoken.JwtException;
import pe.upeu.agrosierra.seguridad.*;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UsuarioRepository usuarios;
    private final Errores errores;
    public JwtAuthenticationFilter(JwtService jwt, UsuarioRepository usuarios, Errores errores) { this.jwt=jwt; this.usuarios=usuarios; this.errores=errores; }
    @Override protected boolean shouldNotFilter(HttpServletRequest r) {
        return r.getMethod().equals("POST") && r.getServletPath().equals("/api/v1/auth/login");
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String header=req.getHeader("Authorization");
        if(header!=null) {
            try {
                if(!header.startsWith("Bearer ")) throw new JwtException("Cabecera invalida");
                Long id=jwt.verificar(header.substring(7));
                Usuario u=usuarios.findById(id).filter(Usuario::getActivo).orElseThrow(() -> new JwtException("Usuario inactivo"));
                // Releer privilegios evita conservar permisos revocados hasta que expire el JWT.
                UsuarioActual actual=UsuarioActual.de(u);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actual,null,actual.authorities()));
            } catch(JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); errores.escribir(req,res,HttpStatus.UNAUTHORIZED,"Token invalido o vencido"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
