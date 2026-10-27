package pe.upeu.agrosierra.config;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import pe.upeu.agrosierra.web.dto.ErrorResponse;
@Component
public class Errores {
    private final ObjectMapper json;
    public Errores(ObjectMapper json) { this.json=json; }
    public ErrorResponse cuerpo(HttpStatus status, String mensaje, String path, Map<String,String> campos) {
        return new ErrorResponse(Instant.now(),status.value(),status.getReasonPhrase(),mensaje,path,campos);
    }
    public void escribir(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String mensaje) throws IOException {
        res.setStatus(status.value()); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json.writeValueAsString(cuerpo(status,mensaje,req.getRequestURI(),Map.of())));
    }
}
