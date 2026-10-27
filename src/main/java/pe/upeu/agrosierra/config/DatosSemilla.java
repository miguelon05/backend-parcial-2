package pe.upeu.agrosierra.config;

import java.time.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.upeu.agrosierra.dominio.*;
import pe.upeu.agrosierra.repositorio.*;
import pe.upeu.agrosierra.seguridad.*;

/** Anexo del examen: solo desarrollo local y pruebas; no disponible en Oracle. */
@Component
@Profile("(local | test) & !oracle")
public class DatosSemilla implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final LoteRepository lotes;
    private final LaborRepository labores;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DatosSemilla(UsuarioRepository usuarios, RolRepository roles, LoteRepository lotes,
            LaborRepository labores, PasswordEncoder encoder, Clock clock) {
        this.usuarios=usuarios; this.roles=roles; this.lotes=lotes; this.labores=labores;
        this.encoder=encoder; this.clock=clock;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        // Conserva modificaciones y eliminaciones en los siguientes arranques.
        if (usuarios.count()!=0 || lotes.count()!=0) return;
        Usuario tecnico=usuario("tecnico1","Tecnico de campo","TECNICO");
        Usuario jefe=usuario("jefe1","Jefe de campo","JEFE_CAMPO");
        usuario("admin","Administrador","ADMINISTRADOR");
        String[] nombres={"La Esperanza","El Mirador","Los Alisos","Santa Rosa","El Molino","La Rinconada","Las Palmeras","El Descanso"};
        String[] valles={"Mantaro","Sagrado","Colca"};
        String[] cultivos={"Papa","Maiz","Quinua","Haba"};
        Instant ahora=clock.instant();
        for(int i=0;i<8;i++) {
            Lote lote=new Lote(); lote.setCodigo("LOT-"+String.format("%03d",i+1));
            lote.setNombre(nombres[i]); lote.setHectareas(2.0+i*0.5); lote.setCultivo(cultivos[i%4]);
            lote.setValle(valles[i%3]); lote.setCampaniaCerrada(i>=6); lote.setDosisMaximaPorHectarea(3.0);
            lotes.saveAndFlush(lote);
            int numero=i<4?3:2;
            for(int j=0;j<numero;j++) {
                Labor labor=new Labor(); labor.setLote(lote);
                boolean cosecha=(i==0 && j==2) || (i>=6 && j==1);
                labor.setTipo(cosecha?TipoLabor.COSECHA:(j%2==0?TipoLabor.RIEGO:TipoLabor.FUMIGACION));
                labor.setFecha(LocalDate.now(clock).minusDays(j+1));
                labor.setResponsable(j%2==0?tecnico:jefe); labor.setCantidad(cosecha?120.0:2.0);
                labor.setObservacion("Labor inicial de demostracion");
                labor.setCreadoEn(ahora.minus(Duration.ofHours(j==0?2:48+j)));
                labores.save(labor);
            }
        }
    }

    private Usuario usuario(String login,String nombre,String rol) {
        Usuario u=new Usuario();u.setUsuario(login);u.setNombre(nombre);
        u.setContrasena(encoder.encode("Agro2026*"));u.setRol(roles.findById(rol).orElseThrow());
        return usuarios.saveAndFlush(u);
    }
}
