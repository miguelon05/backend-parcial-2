package pe.upeu.agrosierra;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import pe.upeu.agrosierra.config.*;
import pe.upeu.agrosierra.dominio.*;
import pe.upeu.agrosierra.repositorio.*;
import pe.upeu.agrosierra.seguridad.*;

/** Peticiones HTTP reales: atraviesan filtros, controladores, servicios, Flyway y JPA. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgroApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired DatosSemilla seed;
    @Autowired LoteRepository lotes;
    @Autowired LaborRepository labores;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    final HttpClient http=HttpClient.newHttpClient();
    String tecnico,jefe,admin;
    List<Lote> padron;
    record Respuesta(int status,String cuerpo) {}

    @BeforeEach void preparar() throws Exception {
        jdbc.update("DELETE FROM labor");jdbc.update("DELETE FROM lote");jdbc.update("DELETE FROM usuario");
        jdbc.update("DELETE FROM rol_privilegio WHERE rol_nombre NOT IN ('TECNICO','JEFE_CAMPO','ADMINISTRADOR')");
        jdbc.update("DELETE FROM rol WHERE nombre NOT IN ('TECNICO','JEFE_CAMPO','ADMINISTRADOR')");
        seed.run(null);
        padron=lotes.findAll().stream().sorted(Comparator.comparing(Lote::getCodigo)).toList();
        tecnico=login("tecnico1");jefe=login("jefe1");admin=login("admin");
    }
    Respuesta enviar(String metodo,String ruta,String token,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+ruta)).timeout(Duration.ofSeconds(15));
        if(token!=null) b.header("Authorization","Bearer "+token);
        b.header("Content-Type","application/json");
        var payload=body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:json.writeValueAsString(body));
        var res=http.send(b.method(metodo,payload).build(),HttpResponse.BodyHandlers.ofString());
        return new Respuesta(res.statusCode(),res.body());
    }
    JsonNode arbol(Respuesta r) { return json.readTree(r.cuerpo()); }
    String login(String u) throws Exception {
        var r=enviar("POST","/auth/login",null,Map.of("usuario",u,"contrasena","Agro2026*"));assertEquals(200,r.status(),r.cuerpo());
        return arbol(r).get("token").asString();
    }
    Long lote(int indice) { return padron.get(indice).getId(); }
    Map<String,Object> peticion(Long id,String tipo,double cantidad) {
        return new HashMap<>(Map.of("loteId",id,"tipo",tipo,"fecha","2026-10-27","cantidad",cantidad,"observacion","Prueba"));
    }
    long crear(String token) throws Exception {
        var r=enviar("POST","/labores",token,peticion(lote(0),"RIEGO",1));assertEquals(201,r.status(),r.cuerpo());return arbol(r).get("id").asLong();
    }
    void estado(int esperado,Respuesta r) { assertEquals(esperado,r.status(),r.cuerpo()); }

    @Test void semillaCompletaYPersistidaConBCrypt() {
        assertEquals(8,lotes.count());assertEquals(20,labores.count());assertEquals(3,usuarios.count());
        assertEquals(2,padron.stream().filter(Lote::getCampaniaCerrada).count());
        assertEquals(3,padron.stream().map(Lote::getValle).distinct().count());
        assertTrue(encoder.matches("Agro2026*",usuarios.findByUsuario("tecnico1").orElseThrow().getContrasena()));
        for(var l:padron) if(!l.getCampaniaCerrada()) assertTrue(labores.findByLoteId(l.getId(),org.springframework.data.domain.PageRequest.of(0,100)).getTotalElements()>=2);
    }
    @Test void semillaNoDuplicaAlReiniciar() { seed.run(null);assertEquals(20,labores.count());assertEquals(3,usuarios.count()); }
    @Test void loginIncluyeSesionCompatibleConFrontendYUnaHora() throws Exception {
        var r=enviar("POST","/auth/login",null,Map.of("usuario","jefe1","contrasena","Agro2026*"));var n=arbol(r);
        assertEquals("JEFE_CAMPO",n.get("rol").asString());assertTrue(n.get("editaTodas").asBoolean());assertFalse(n.get("nombre").asString().isBlank());
        long segundos=n.get("expiraEn").asLong()-Instant.now().getEpochSecond();assertTrue(segundos>=3595 && segundos<=3600);
        assertEquals(usuarios.findByUsuario("jefe1").orElseThrow().getId(),jwt.verificar(n.get("token").asString()));
    }
    @Test void credencialesIncorrectasDan401() throws Exception { estado(401,enviar("POST","/auth/login",null,Map.of("usuario","tecnico1","contrasena","mala"))); }
    @Test void todasLasRutasRequierenToken() throws Exception {
        for(String ruta:List.of("/lotes","/lotes/1","/lotes/1/labores","/usuarios/me","/usuarios","/roles")) estado(401,enviar("GET",ruta,null,null));
        estado(401,enviar("POST","/labores",null,peticion(lote(0),"RIEGO",1)));
        estado(401,enviar("PUT","/labores/1",null,peticion(lote(0),"RIEGO",1)));
        estado(401,enviar("DELETE","/labores/1",null,null));
        estado(401,enviar("POST","/lotes/1/cerrar-campania",null,null));
    }
    @Test void tokenAlteradoVencidoYMalformadoDan401() throws Exception {
        String alterado=tecnico.substring(0,tecnico.length()/2)+"X"+tecnico.substring(tecnico.length()/2+1);
        for(String t:List.of(alterado,"no-es-un-token",jwt.crear(usuarios.findByUsuario("tecnico1").orElseThrow(),Instant.now().minusSeconds(60)))) estado(401,enviar("GET","/lotes",t,null));
    }
    @Test void usuarioDesactivadoPierdeAcceso() throws Exception {
        var u=usuarios.findByUsuario("tecnico1").orElseThrow();u.setActivo(false);usuarios.save(u);estado(401,enviar("GET","/lotes",tecnico,null));
    }
    @Test void perfilNoExponeContrasena() throws Exception { var r=enviar("GET","/usuarios/me",tecnico,null);estado(200,r);assertEquals("tecnico1",arbol(r).get("usuario").asString());assertNull(arbol(r).get("contrasena")); }
    @Test void paginacionFiltrosYBusqueda() throws Exception {
        var r=enviar("GET","/lotes?pagina=1&tamanio=3",tecnico,null);estado(200,r);var n=arbol(r);
        assertEquals(8,n.get("totalElementos").asLong());assertEquals(3,n.get("totalPaginas").asInt());assertEquals(3,n.get("contenido").size());assertFalse(n.get("ultima").asBoolean());
        var filtro=arbol(enviar("GET","/lotes?valle=Mantaro&campaniaCerrada=false",tecnico,null));assertEquals(2,filtro.get("contenido").size());
        assertEquals(1,arbol(enviar("GET","/lotes?busqueda=LOT-001",tecnico,null)).get("contenido").size());
        var vacia=arbol(enviar("GET","/lotes?pagina=100",tecnico,null));assertEquals(0,vacia.get("contenido").size());assertTrue(vacia.get("ultima").asBoolean());
    }
    @Test void paginacionYParametrosInvalidosDan400() throws Exception {
        for(String q:List.of("pagina=-1","tamanio=0","tamanio=101","pagina=abc","campaniaCerrada=abc")) estado(400,enviar("GET","/lotes?"+q,tecnico,null));
    }
    @Test void loteYLaboresInexistentesDan404() throws Exception {
        estado(404,enviar("GET","/lotes/999999",tecnico,null));estado(404,enviar("GET","/lotes/999999/labores",tecnico,null));
        estado(404,enviar("DELETE","/labores/999999",jefe,null));estado(404,enviar("PUT","/labores/999999",jefe,peticion(lote(0),"RIEGO",1)));
    }
    @Test void laboresOrdenadasPorFechaDescendente() throws Exception {
        var r=enviar("GET","/lotes/"+lote(0)+"/labores?tamanio=2",tecnico,null);estado(200,r);var n=arbol(r);
        assertEquals(3,n.get("totalElementos").asLong());assertEquals(2,n.get("contenido").size());
        assertTrue(n.get("contenido").get(0).get("fecha").asString().compareTo(n.get("contenido").get(1).get("fecha").asString())>=0);
    }
    @Test void CRUDRealYResponsableSaleDelToken() throws Exception {
        var p=peticion(lote(0),"RIEGO",1);p.put("responsable","admin");var r=enviar("POST","/labores",tecnico,p);estado(201,r);
        var n=arbol(r);assertEquals("tecnico1",n.get("responsable").asString());assertFalse(n.get("creadoEn").asString().isBlank());long id=n.get("id").asLong();
        assertTrue(labores.existsById(id));p.put("cantidad",3);
        r=enviar("PUT","/labores/"+id,tecnico,p);estado(200,r);assertEquals(3,arbol(r).get("cantidad").asDouble());
        r=enviar("DELETE","/labores/"+id,jefe,null);estado(204,r);assertEquals("",r.cuerpo());assertFalse(labores.existsById(id));
    }
    @Test void adminLeePeroNoRegistraEditaEliminaOCierra() throws Exception {
        estado(200,enviar("GET","/lotes",admin,null));long id=crear(tecnico);
        estado(403,enviar("POST","/labores",admin,peticion(lote(0),"RIEGO",1)));
        estado(403,enviar("PUT","/labores/"+id,admin,peticion(lote(0),"RIEGO",1)));
        estado(403,enviar("DELETE","/labores/"+id,admin,null));
        estado(403,enviar("POST","/lotes/"+lote(0)+"/cerrar-campania",admin,null));
    }
    @Test void tecnicoNoEliminaNiCierraNiGestionaUsuarios() throws Exception {
        long id=crear(tecnico);estado(403,enviar("DELETE","/labores/"+id,tecnico,null));
        estado(403,enviar("POST","/lotes/"+lote(0)+"/cerrar-campania",tecnico,null));
        estado(403,enviar("GET","/usuarios",tecnico,null));estado(200,enviar("GET","/usuarios/me",tecnico,null));
    }
    @Test void RN01CampaniaCerradaRechazaCreacionYEdicion() throws Exception {
        estado(409,enviar("POST","/labores",tecnico,peticion(lote(6),"RIEGO",1)));
        var l=labores.findAll().stream().filter(x->x.getLote().getId().equals(lote(6))).findFirst().orElseThrow();
        estado(409,enviar("PUT","/labores/"+l.getId(),jefe,peticion(lote(6),"RIEGO",1)));
    }
    @Test void RN02TecnicoNoEditaLaboresAjenas() throws Exception {
        long id=crear(jefe);estado(403,enviar("PUT","/labores/"+id,tecnico,peticion(lote(0),"RIEGO",1)));
    }
    @Test void RN02TecnicoNoEditaTras24HorasPeroJefeSi() throws Exception {
        long id=crear(tecnico);var l=labores.findById(id).orElseThrow();l.setCreadoEn(Instant.now().minus(Duration.ofHours(25)));labores.save(l);
        estado(403,enviar("PUT","/labores/"+id,tecnico,peticion(lote(0),"RIEGO",2)));
        estado(200,enviar("PUT","/labores/"+id,jefe,peticion(lote(0),"RIEGO",2)));
    }
    @Test void RN03CierreExigeCosechaYPersiste() throws Exception {
        estado(409,enviar("POST","/lotes/"+lote(1)+"/cerrar-campania",jefe,null));
        estado(200,enviar("POST","/lotes/"+lote(0)+"/cerrar-campania",jefe,null));
        assertTrue(lotes.findById(lote(0)).orElseThrow().getCampaniaCerrada());
        estado(409,enviar("POST","/labores",tecnico,peticion(lote(0),"RIEGO",1)));
    }
    @Test void RN04DosisMaximaSeValidaEnAltaYEdicion() throws Exception {
        var r=enviar("POST","/labores",tecnico,peticion(lote(0),"FUMIGACION",6.01));estado(400,r);assertNotNull(arbol(r).get("validationErrors").get("cantidad"));
        estado(201,enviar("POST","/labores",tecnico,peticion(lote(0),"FUMIGACION",6)));
        long id=crear(tecnico);estado(400,enviar("PUT","/labores/"+id,jefe,peticion(lote(0),"FUMIGACION",7)));
    }
    @Test void validacionDeCamposYFormatoComun() throws Exception {
        var r=enviar("POST","/labores",tecnico,Map.of("cantidad",-2,"observacion","x"));estado(400,r);var n=arbol(r);
        for(String campo:List.of("timestamp","status","error","message","path","validationErrors")) assertNotNull(n.get(campo));
        for(String campo:List.of("loteId","tipo","fecha","cantidad")) assertNotNull(n.get("validationErrors").get(campo));
        assertEquals("/api/v1/labores",n.get("path").asString());
    }
    @Test void jsonInvalidoYEnumsInvalidosDan400() throws Exception {
        estado(400,enviar("POST","/labores",tecnico,"{mal"));
        var r=enviar("POST","/labores",tecnico,peticion(lote(0),"OTRO",1));estado(400,r);assertNotNull(arbol(r).get("validationErrors").get("tipo"));
        var p=peticion(lote(0),"RIEGO",1);p.put("fecha","ayer");r=enviar("POST","/labores",tecnico,p);estado(400,r);assertNotNull(arbol(r).get("validationErrors").get("fecha"));
    }
    @Test void noPermiteTrasladarLaborAOtroLote() throws Exception { long id=crear(tecnico);estado(400,enviar("PUT","/labores/"+id,jefe,peticion(lote(1),"RIEGO",1))); }
    @Test void claveClienteEvitaDuplicadosYDetectaCambioDePeticion() throws Exception {
        var p=peticion(lote(0),"RIEGO",1);p.put("claveCliente","offline-123");
        var a=enviar("POST","/labores",tecnico,p);var b=enviar("POST","/labores",tecnico,p);estado(201,a);estado(201,b);
        assertEquals(arbol(a).get("id").asLong(),arbol(b).get("id").asLong());assertEquals(21,labores.count());
        p.put("cantidad",2);estado(409,enviar("POST","/labores",tecnico,p));
    }
    @Test void reintentoTrasCierreDevuelveLaborYaCreada() throws Exception {
        var p=peticion(lote(0),"RIEGO",1);p.put("claveCliente","offline-cierre");var a=enviar("POST","/labores",tecnico,p);
        estado(200,enviar("POST","/lotes/"+lote(0)+"/cerrar-campania",jefe,null));
        var b=enviar("POST","/labores",tecnico,p);estado(201,b);assertEquals(arbol(a).get("id").asLong(),arbol(b).get("id").asLong());
    }
    @Test void reintentosConcurrentesNoDuplican() throws Exception {
        var p=peticion(lote(0),"RIEGO",1);p.put("claveCliente","offline-concurrente");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var a=executor.submit(()->enviar("POST","/labores",tecnico,p));var b=executor.submit(()->enviar("POST","/labores",tecnico,p));
            var ra=a.get();var rb=b.get();estado(201,ra);estado(201,rb);assertEquals(arbol(ra).get("id").asLong(),arbol(rb).get("id").asLong());assertEquals(21,labores.count());
        }
    }
    @Test void adminGestionaUsuariosRolesYRevocacionInmediata() throws Exception {
        var r=enviar("POST","/usuarios",admin,Map.of("usuario","tecnico2","contrasena","Prueba2026*","nombre","Tecnico dos","rol","TECNICO"));estado(201,r);
        long id=arbol(r).get("id").asLong();
        estado(200,enviar("PUT","/usuarios/"+id+"/rol",admin,Map.of("rol","JEFE_CAMPO")));
        estado(201,enviar("POST","/roles",admin,Map.of("nombre","LECTOR","privilegios",List.of("LOTE_LEER"),"editaTodas",false)));
        estado(200,enviar("GET","/roles",admin,null));
        var u=usuarios.findByUsuario("tecnico1").orElseThrow();
        estado(200,enviar("PUT","/usuarios/"+u.getId()+"/rol",admin,Map.of("rol","LECTOR")));
        estado(403,enviar("POST","/labores",tecnico,peticion(lote(0),"RIEGO",1)));
        estado(200,enviar("GET","/lotes",tecnico,null));
    }
}
