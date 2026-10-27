package pe.upeu.agrosierra.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.upeu.agrosierra.seguridad.Usuario;
@Service
public class JwtService {
    private final SecretKey key;
    private final Clock clock;
    public JwtService(@Value("${agro.jwt.secret}") String secret, Clock clock) {
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.clock=clock;
    }
    public String crear(Usuario u, Instant expira) {
        return Jwts.builder().issuer("AgroAPI").subject(u.getId().toString())
            .claim("usuario",u.getUsuario()).claim("privilegios",u.getRol().getPrivilegios().stream().map(Enum::name).sorted().toList())
            .issuedAt(Date.from(clock.instant())).expiration(Date.from(expira)).signWith(key).compact();
    }
    public Long verificar(String token) {
        Claims claims=Jwts.parser().verifyWith(key).requireIssuer("AgroAPI")
            .clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
        if(claims.getExpiration()==null || claims.getIssuedAt()==null) throw new JwtException("Token sin vigencia");
        return Long.valueOf(claims.getSubject());
    }
}
