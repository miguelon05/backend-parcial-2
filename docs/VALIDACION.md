# Validacion de la entrega

Ejecutada el 27 de octubre de 2026 con JDK 21 y Spring Boot 4.0.7.

## Resultados

- `mvnw.cmd package`: compilacion y empaquetado del JAR ejecutable correctos.
- 27 pruebas de integracion HTTP: 0 fallos, 0 errores, 0 omitidas.
- 28 peticiones de la coleccion Postman ejecutadas contra el JAR y una base H2 temporal: todos los codigos esperados correctos.
- Arranque con perfil local y migraciones Flyway correctos.
- Reinicio del JAR con la misma base en disco: conserva el cierre de campania, el usuario creado y los 8 lotes. Los tokens emitidos antes del reinicio siguen siendo validos dentro de su vigencia.
- OpenAPI responde correctamente al enviar el token.

Las pruebas automaticas usan una base en memoria aislada; la coleccion se verifico con una base en disco temporal para no alterar los datos de demostracion de `data/agro`.

## Cobertura

| Comportamiento | Verificacion |
| --- | --- |
| Semilla de 8 lotes, 20 labores, 3 usuarios, 3 valles, 2 campanias cerradas | Prueba de conteos, permisos y BCrypt |
| JWT de una hora; token alterado, vencido y malformado | Respuestas HTTP 200/401 |
| Todas las rutas del contrato protegidas | Peticiones sin token -> 401 |
| Permisos del tecnico, jefe y administrador | Operaciones directas permitidas o rechazadas con 403 |
| Paginacion, filtros y orden de labores | Contenido, totales y fechas verificadas |
| Crear, corregir y eliminar | Cambios comprobados en la base y DELETE 204 sin cuerpo |
| RN-01 | Alta y correccion en campania cerrada -> 409 |
| RN-02 | Labor ajena o mas de 24 horas -> 403; jefe puede corregir |
| RN-03 | Sin cosecha -> 409; con cosecha -> cierre persistido |
| RN-04 | Exceso de fumigacion -> 400 y detalle bajo cantidad |
| Errores del contrato | Campos comunes, JSON invalido y validacion de campos |
| Sincronizacion | Misma clave y cuerpo devuelven mismo id, incluso en altas concurrentes |
| Administracion | Crear usuario, asignar rol y revocar un permiso con token aun vigente |

## Limites de esta verificacion

El perfil Oracle esta configurado, pero no se ha conectado a una instancia Oracle real. La aplicacion Android no se modifico ni se ejecuto desde este backend; se verifico el contrato de sus DTO y rutas leyendo el proyecto existente. La comprobacion visual de la aplicacion, sus pruebas commonTest y las capturas del emulador pertenecen al repositorio Android.

El historial Git contiene fechas reales de esta sesion. No se crearon PR remotos, evidencias de desarrollo anterior ni solicitudes de cambio de la Parte II.
