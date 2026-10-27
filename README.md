# AgroAPI - AgroSierra

Backend REST para la aplicacion Android **AgroCampo** del proyecto `ACEVEDO-PARCIAL 2`. Implementa la Parte I del caso AgroSierra: JWT, privilegios, padron de lotes, CRUD de labores, cierre de campania y persistencia relacional. No contiene frontend ni modifica el proyecto Android.

## Ejecutar en Windows

Requiere JDK 21. Maven Wrapper descarga Maven y las dependencias la primera vez.

```powershell
.\iniciar.ps1
```

Alternativa desde IntelliJ o terminal con `JAVA_HOME` configurado:

```powershell
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080/api/v1/`. El perfil predeterminado `local` usa una base **H2 en disco**, en `data/agro.mv.db`; conserva las operaciones al reiniciar. Flyway crea el esquema y los privilegios; una transaccion inicial inserta los datos de demostracion si la base esta vacia. No se necesita instalar una base de datos para esta demostracion.

El servidor escucha en `127.0.0.1`. El emulador Android accede mediante `http://10.0.2.2:8080/api/v1/`, que es la direccion configurada en el frontend existente. Para otro puerto: `.\iniciar.ps1 -Puerto 8082` y cambiar la direccion del cliente cuando corresponda. Para un dispositivo fisico se debe configurar expresamente `SERVER_ADDRESS` y la IP de la computadora en el cliente.

## Usuarios de demostracion local

| Usuario | Contrasena | Rol |
| --- | --- | --- |
| tecnico1 | Agro2026* | TECNICO |
| jefe1 | Agro2026* | JEFE_CAMPO |
| admin | Agro2026* | ADMINISTRADOR |

Estas cuentas pertenecen al anexo del examen. Se cargan exclusivamente en `local` o `test`, nunca si esta activo `oracle`. La contrasena se guarda como hash BCrypt. La clave JWT local es de demostracion; en Oracle se exige `JWT_SECRET` externo. No usar el perfil local para un despliegue publico.

Datos iniciales: 8 lotes en Mantaro, Sagrado y Colca; 2 con campania cerrada; 20 labores; cada lote abierto tiene al menos 2 labores. `LOT-001` tiene cosecha y `LOT-002` no tiene cosecha, para demostrar RN-03. Los identificadores se asignan en la base de datos. Los datos semilla no se vuelven a insertar al reiniciar una base ya poblada.

## Estructura y referencia

Se sigue la arquitectura por capas de [dreyna/pharmaSoft](https://github.com/dreyna/pharmaSoft), con Java 21, Spring Boot 4.0.7, Spring Security, JPA, Flyway y JWT firmado. El PDF muestra nombres de archivos Kotlin como estructura esperada; aqui se usa **Java como en el backend de referencia solicitado**, manteniendo la separacion de responsabilidades y el contrato JSON del examen.

```text
src/main/java/pe/upeu/agrosierra/
  Aplicacion.java
  config/          seguridad HTTP, JWT, errores, OpenAPI, datos semilla
  seguridad/       usuarios, roles, privilegios y principal autenticado
  dominio/         Lote, Labor y TipoLabor
  repositorio/     consultas JPA de lotes y labores
  servicio/        contratos, autenticacion y gestion de usuarios
    impl/          transacciones y reglas RN-01 a RN-04
  web/             controladores REST
    dto/           solicitudes, respuestas, paginacion y errores
    mapper/        conversion entidad -> respuesta
src/main/resources/db/migration/  esquema y matriz de privilegios
src/test/                        pruebas HTTP con base H2 aislada
postman/                         coleccion importable
```

Los controladores reciben DTO y delegan. Los servicios verifican `hasAuthority(...)`, coordinan transacciones y aplican las reglas de negocio. Los repositorios persisten y consultan entidades. Las entidades nunca se serializan como respuesta de labores o usuarios; no se exponen hashes de contrasena.

## Autenticacion y autorizacion

`POST /api/v1/auth/login` recibe `usuario` y `contrasena`. La respuesta incluye `token`, `usuario`, `nombre`, `rol`, `privilegios`, `expiraEn` (Unix, segundos) y `editaTodas`, compatibles con los DTO del frontend. El JWT contiene el identificador del usuario y sus privilegios, tiene firma HMAC y vence una hora despues de emitirse. No hay renovacion.

Enviar `Authorization: Bearer <token>` en las siguientes peticiones. **Todas las rutas salvo POST login requieren autenticacion**, incluidas la documentacion y las rutas desconocidas. El filtro verifica firma, emisor y expiracion, comprueba que el usuario siga activo y relee sus permisos de la base; una revocacion tiene efecto aunque el token aun no venza. Los errores del filtro conservan el mismo contrato JSON que los errores de los servicios.

| Privilegio | TECNICO | JEFE_CAMPO | ADMINISTRADOR |
| --- | --- | --- | --- |
| LOTE_LEER | Si | Si | Si |
| LABOR_LEER | Si | Si | Si |
| LABOR_CREAR | Si | Si | No |
| LABOR_EDITAR | Propias, menos de 24 h | Todas | No |
| LABOR_ELIMINAR | No | Si | No |
| CAMPANIA_CERRAR | No | Si | No |
| USUARIO_GESTIONAR | No | No | Si |

La matriz vive en tablas `rol` y `rol_privilegio`. `editaTodas` es una politica del rol almacenada en la base, no una comparacion de nombres dentro de los controladores. Tenerla en `true` no concede LABOR_EDITAR: hace falta tambien ese privilegio. La app usa el mismo dato para ofrecer la edicion adecuada.

## Contrato REST

Prefijo de todas las rutas: `/api/v1`.

| Verbo | Ruta | Permiso | Exito |
| --- | --- | --- | --- |
| POST | /auth/login | Publico | 200 |
| GET | /lotes | LOTE_LEER | 200 |
| GET | /lotes/{id} | LOTE_LEER | 200 |
| GET | /lotes/{id}/labores | LABOR_LEER | 200 |
| POST | /labores | LABOR_CREAR | 201 |
| PUT | /labores/{id} | LABOR_EDITAR | 200 |
| DELETE | /labores/{id} | LABOR_ELIMINAR | 204 sin cuerpo |
| POST | /lotes/{id}/cerrar-campania | CAMPANIA_CERRAR | 200 |
| GET | /usuarios/me | Autenticado | 200 |
| GET | /usuarios | USUARIO_GESTIONAR | 200 |
| POST | /usuarios | USUARIO_GESTIONAR | 201 |
| PUT | /usuarios/{id}/rol | USUARIO_GESTIONAR | 200 |
| GET | /roles | USUARIO_GESTIONAR | 200 |
| POST | /roles | USUARIO_GESTIONAR | 201 |

Todos los listados usan `pagina` (desde 0) y `tamanio` (1 a 100; predeterminado 20). Lotes admite filtros combinables `valle` (igualdad sin distinguir mayusculas), `campaniaCerrada=true|false` y `busqueda` (nombre o codigo). Las labores se ordenan por fecha descendente, despues por fecha de registro e id, para obtener un orden estable.

```json
{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":0,"totalPaginas":0,"ultima":true}
```

Crear o corregir una labor:

```json
{
  "loteId": 1,
  "tipo": "RIEGO",
  "fecha": "2026-10-27",
  "cantidad": 2.5,
  "observacion": "Riego de la manana",
  "claveCliente": "identificador-unico-generado-por-la-app"
}
```

`tipo` solo admite RIEGO, FUMIGACION o COSECHA. La fecha es ISO `yyyy-MM-dd`; cantidad debe ser positiva y finita; observacion es obligatoria, puede ser vacia y tiene un maximo de 1000 caracteres. El responsable se obtiene del usuario autenticado, nunca del cuerpo. Una correccion conserva el responsable y la fecha de creacion; no permite mover la labor a otro lote.

La respuesta incluye `id`, `loteId`, `tipo`, `fecha`, `responsable` (login), `cantidad`, `observacion` y `creadoEn` (instante UTC ISO). `claveCliente` es opcional y se usa en altas offline: repetir la misma solicitud con el mismo usuario y clave devuelve el mismo id con 201; cambiar los datos usando esa clave produce 409. El bloqueo y la restriccion unica evitan duplicados concurrentes. Los reintentos de una alta ya registrada siguen devolviendo su resultado aunque despues se cierre la campania. La clave no es un mecanismo de sincronizacion incremental ni un historial de versiones.

Reglas en `servicio/impl`:

- RN-01: alta y correccion sobre campania cerrada -> 409.
- RN-02: sin `editaTodas`, solo el responsable puede corregir antes de cumplir 24 horas desde `creadoEn` -> 403. El plazo usa el reloj del servidor en UTC.
- RN-03: cierre sin una COSECHA -> 409. Un cierre ya efectuado responde 409.
- RN-04: FUMIGACION mayor que hectareas por dosis maxima -> 400 y `validationErrors.cantidad`.

Las operaciones de labores y cierre bloquean el lote durante la transaccion para evitar registros que se intercalen con el cierre. RN-01 no prohibe eliminar: DELETE sigue el privilegio LABOR_ELIMINAR del contrato del examen.

Formato comun de error:

```json
{
  "timestamp":"2026-10-27T14:14:22Z",
  "status":400,
  "error":"Bad Request",
  "message":"Datos invalidos",
  "path":"/api/v1/labores",
  "validationErrors":{"cantidad":"Debe ser mayor que 0"}
}
```

400 indica validacion; 401 credenciales o token incorrectos, ausentes o vencidos; 403 falta de privilegio o RN-02; 404 recurso inexistente; 409 conflicto o regla de negocio; 500 un fallo inesperado, sin exponer detalles internos. JSON mal formado, enum desconocido, fecha incompatible y parametros invalidos se traducen a 400.

## Probar con Postman

Importar `postman/AgroAPI.postman_collection.json`. Ejecutar primero los tres inicios de sesion: guardan los tokens en variables de la coleccion. El listado de lotes guarda ids para las demostraciones. La coleccion incluye operaciones y comprobaciones de 401, 403, 400, RN-01, RN-02 y RN-03. Los cierres y eliminaciones cambian datos de la base; los escenarios se describen junto a cada peticion.

OpenAPI esta en `/v3/api-docs` y Swagger en `/swagger-ui.html`; requieren token como todas las rutas protegidas. Para obtener el JSON se puede usar Postman con el bearer. La navegacion normal a Swagger no agrega automaticamente la cabecera: usar un cliente que envie Authorization tambien para esos recursos.

## Pruebas automatizadas

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Las pruebas levantan un servidor real en puerto aleatorio y una H2 en memoria: no modifican `data/agro`. Verifican semilla, BCrypt, JWT alterado y vencido, autorizacion por rol, CRUD persistido, 204, paginacion, filtros, errores por campo, RN-01 a RN-04, reintentos concurrentes, gestion de usuarios y revocacion de privilegios. Los informes quedan en `target/surefire-reports/`.

El JAR ejecutable se genera en `target/agro-api-1.0.0.jar` y arranca con `java -jar target/agro-api-1.0.0.jar`.

## Perfil Oracle

Crear previamente un esquema vacio con permisos para crear tablas, indices y secuencias. Configurar valores propios en la terminal:

```powershell
$env:SPRING_PROFILES_ACTIVE='oracle'
$env:DB_URL='jdbc:oracle:thin:@localhost:1521/FREEPDB1'
$env:DB_USERNAME='AGROSIERRA'
$env:DB_PASSWORD='<contrasena-del-esquema>'
$env:JWT_SECRET='<clave-aleatoria-de-al-menos-32-bytes>'
.\mvnw.cmd spring-boot:run
```

Flyway crea el esquema y los roles. Oracle no carga cuentas de demostracion automaticamente: se deben aprovisionar usuarios con hashes BCrypt y los datos de negocio antes de utilizar ese perfil. Para demostrar el examen sin aprovisionamiento adicional usar `local`. El driver y las migraciones Oracle estan incluidos; las pruebas automaticas usan H2 con su propia migracion, que no sustituye una validacion contra una instancia Oracle real.

## Git y alcance de la entrega

El historial local registra trabajo de esta sesion con fechas reales. No acredita desarrollo durante semanas anteriores, solicitudes de incorporacion cerradas en GitHub ni una defensa individual. No se inventan esas evidencias. No hay remoto publicado automaticamente. La Parte II requiere que el docente asigne SC-A, SC-B, SC-C o SC-D; no se elige una solicitud por cuenta propia.

El frontend existente se leyo para verificar rutas, nombres JSON, pagina maxima 100, responsable por login, `expiraEn`, `editaTodas` y `claveCliente`. Las pruebas HTTP validan ese contrato; la demostracion completa en un emulador y las capturas de la app corresponden al proyecto Android.
