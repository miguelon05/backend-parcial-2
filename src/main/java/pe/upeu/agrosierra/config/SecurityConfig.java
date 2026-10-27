package pe.upeu.agrosierra.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import java.time.Clock;
import java.util.*;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http,JwtAuthenticationFilter jwt,Errores errores) throws Exception {
        return http.csrf(c -> c.disable()).cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/auth/login").permitAll().anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((r,s,x) -> errores.escribir(r,s,HttpStatus.UNAUTHORIZED,"Se requiere un token valido"))
                .accessDeniedHandler((r,s,x) -> errores.escribir(r,s,HttpStatus.FORBIDDEN,"No tiene el privilegio necesario")))
            .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> filtroSoloSeguridad(JwtAuthenticationFilter filtro) {
        var registro=new FilterRegistrationBean<>(filtro); registro.setEnabled(false); return registro;
    }
    @Bean CorsConfigurationSource cors(@Value("${agro.cors-origins}") String origins) {
        var c=new CorsConfiguration(); c.setAllowedOrigins(Arrays.asList(origins.split(",")));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type")); c.setExposedHeaders(List.of("Location"));
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",c); return source;
    }
}
