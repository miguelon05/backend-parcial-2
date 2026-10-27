package pe.upeu.agrosierra.config;

import org.springframework.http.HttpStatus;
import java.util.Map;
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final Map<String,String> campos;
    public ApiException(HttpStatus status, String mensaje) { this(status, mensaje, Map.of()); }
    public ApiException(HttpStatus status, String mensaje, Map<String,String> campos) {
        super(mensaje); this.status=status; this.campos=campos;
    }
}
