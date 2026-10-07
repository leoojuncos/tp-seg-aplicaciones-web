# Reglas del repo

Reglas de trabajo para todo el equipo, tanto para quien trabaja a mano como para los agentes. OpenCode lee este archivo directamente y Claude Code lo carga a través de `CLAUDE.md`, así que las reglas se editan solo acá.

## Commits

Se siguen los [Semantic Commit Messages](https://gist.github.com/joshbuchea/6f47e86d2510bce28f8e7f42ae84c716), con la clave del ticket de Jira adelante. La clave es obligatoria en todos los commits.

```
TPS-<número> - <tipo>: <descripción>
```

- La descripción va en español, en infinitivo (agregar, corregir, eliminar...), empieza en minúscula y no lleva punto final.
- El scope del gist es opcional y va pegado al tipo: `TPS-12 - feat(tesoreria): agregar listado de deudas`.
- Los cambios que no corresponden a ningún ticket (configuración del repo, estas reglas, etc.) llevan `NO-TICKET` en lugar de la clave: `NO-TICKET - docs: agregar reglas del repo`.
- Si no está claro si el cambio tiene ticket o cuál es, preguntar antes de commitear. Nunca inventar una clave ni usar `NO-TICKET` para algo que tiene ticket.

Tipos:

- `feat`: funcionalidad nueva para el usuario (no una del build o de los scripts).
- `fix`: corrección de un bug que **ya está en `main`** (del producto, no de un script de build). Si el error está en un commit de la branch que todavía no se mergeó, no se hace un `fix`: se corrige ese commit (ver la última sección).
- `docs`: cambios en la documentación.
- `style`: formato (espacios, indentación, punto y coma, etc.). Sin cambios en el código de producción.
- `refactor`: refactor del código de producción, por ejemplo renombrar una variable.
- `test`: agregar tests faltantes o refactorizar tests. Sin cambios en el código de producción.
- `chore`: build, dependencias, configuración y otras tareas de mantenimiento. Sin cambios en el código de producción.

Ejemplos:

```
TPS-12 - feat: agregar login de usuarios
TPS-18 - fix: corregir el total de deuda en Tesorería
TPS-20 - docs: documentar cómo levantar el entorno local
```

## Títulos de PR

La clave del ticket (o `NO-TICKET`, con el mismo criterio que en los commits) y un título que resuma el PR completo (no el primer commit), en español y con mayúscula inicial. No lleva el tipo del commit.

```
TPS-<número> - <Título representativo>
```

Ejemplo: `TPS-123 - Agregar nueva funcionalidad en Tesorería`.

## Cambios sobre commits que todavía no están en `main`

Si hay que cambiar algo que ya se commiteó pero todavía no se mergeó a `main` (un error propio, un cambio pedido en la revisión del PR, varios retoques chicos después de commitear), no se agrega un commit nuevo: se corrige el commit original con `--amend` o `--fixup` y, si la branch ya estaba pusheada, se hace force-push. Así a `main` no llegan commits de "arreglo lo anterior". El trabajo nuevo de la branch sí va en commits nuevos.

Si el cambio va en el último commit:

```bash
git add <archivos>
git commit --amend --no-edit
git push --force-with-lease
```

Si va en un commit anterior de la branch:

```bash
git add <archivos>
git commit --fixup=<sha-del-commit-a-corregir>
git -c sequence.editor=: rebase -i --autosquash origin/main
git push --force-with-lease
```

- `-c sequence.editor=:` hace que el rebase no abra el editor. El `-i` hace falta: en Git 2.43, por ejemplo, `--autosquash` sin `-i` se ignora sin avisar.
- Antes del push, `git log --oneline origin/main..HEAD` no tiene que mostrar ningún commit `fixup!`.
- Usar `--force-with-lease` y no `--force`: si alguien pusheó a la branch algo que no está en la copia local, el push falla en vez de pisarlo.
- Nunca reescribir ni hacer force-push de `main`.

## Convenciones de código

Reglas cortas; el detalle está en el código que se nombra como ejemplo. Son un estado base: el ticket que necesite cambiar una la cambia en su PR y actualiza esta sección.

### Back (monolito y Auditoría)

- Un módulo es un paquete nombrado como el primer segmento de su ruta después de `/api` (`auth`, `administracion`, `tesoreria`, `vep`, `messaging`), con las capas de `messaging`: `model`, `repository`, `service`, `controller`, `dto` (records) y `mapper` (uno por módulo). Los paquetes raíz son para lo transversal: `config`, `controller` (health), `service` (conectividad), `exception`, `dto`, `filter` y `util`.
- Auditoría es un solo módulo: sus capas (`model`, `repository`, `service`, `controller`, `dto` y `mapper`) y el `listener` de la cola van directo en los paquetes raíz, junto con lo transversal.
- Una entidad se crea una sola vez, en el módulo del ticket que la introduce (`User`, `Role` y `Permission` en `auth`; `Debt` en `tesoreria`). Otro módulo la importa de ahí, y puede usar también sus repositories y services; nunca sus controllers.
- Rutas: `/api/<modulo>/<recurso>`, recursos en inglés y en plural, detalle con parámetro, acciones como `POST /.../{id}/<verbo>`. Públicas: `/api/health`, `/api/auth/**` y `/api/vep/**`; `/api/messaging/**` tiene su propia sesión. El resto exige sesión y el permiso del módulo en mayúscula (lo aplica TPS-15).
- Respuestas: un listado es un array y un detalle un objeto, sin envoltorio. Los filtros de una búsqueda van como query params opcionales del GET.
- Errores: el controller no atrapa excepciones. El service valida y tira `NotFoundException`, `ForbiddenException` o `ConflictException`, y `ApiExceptionHandler` las traduce al formato de `docs/contracts.md`. Los filtros escriben ese formato ellos mismos.
- Tests: de API, con `@SpringBootTest` + MockMvc contra la base real, como `MessagingApiTest`.

### Front

- Vistas en `src/views/<Nombre>View/index.jsx`, exportadas desde `src/views/index.js`; componentes en `src/components/common/<Nombre>/index.jsx`, cada uno con un comentario arriba que dice qué hace y qué props recibe. Archivos y componentes en inglés; rutas y textos en castellano, con voseo.
- Rutas en `src/routes/public.js` y `private.js` (`code` = permiso del módulo; `menu: true` la pone en el menú del encabezado, que dentro de un módulo muestra solo las opciones de ese módulo: los módulos se eligen en `/welcome`; `placeholder: true` la resuelve a "Sección fuera de la demo"). Cada módulo entra por la pantalla que figura en `src/routes/modules.js` y usa su prefijo: listado en plural y detalle en singular con parámetro (`/tesoreria/deudas`, `/tesoreria/deuda/:cuit`).
- HTTP solo con `src/api/http.js` (`getJson`, `postJson`, …), con rutas relativas por el proxy. Formularios con `useForm`; permisos con `usePermissions`. Un listado se arma como `StatusView`: `SectionHeading` + `Message` + `Table`.
- Estilos: Bootstrap con la paleta de `src/styles/theme.css`: verde para confirmar (`btn-primary`), gris para volver o cancelar (`btn-outline-secondary`) y rojo para errores y lo destructivo (`btn-outline-danger`). Sin clases propias de botones (salvo `.btn-icon`, el de solo ícono de las filas de `Table`), sin toasts, sin imágenes.

### Documentación

- README raíz y de cada servicio: qué es, cómo se levanta, cómo se prueba, variables de entorno.
- `AGENTS.md`: cómo se trabaja y las convenciones de código.
- `docs/contracts.md`: lo que una parte le promete a otra (formatos, endpoints entre servicios, cuentas).
- Lo que no entra ahí (props de un componente, lista de pantallas, detalle de una clase) vive en el código, en un comentario arriba. El PR que cambia algo descripto en un documento lo actualiza en el mismo PR.

## Vulnerabilidades del escenario

El SGM es una aplicación didáctica: corre solo en local y es deliberadamente vulnerable, como OWASP Juice Shop o DVWA, para mostrar en clase vulnerabilidades del OWASP Top 10 y cómo se mitigan. Esta lista es la referencia para el equipo, para los agentes y para la review automática de PRs.

- Una vulnerabilidad de la lista no se corrige, salvo en el cambio que implementa su mitigación.
- Una vulnerabilidad que no está en la lista es un defecto, aunque parezca intencional: se corrige o, con el acuerdo del equipo, se suma a la lista.
- Sumar, sacar o cambiar una entrada es una decisión del escenario: va en su propio commit y se explica en la sección `Aclaraciones` de la descripción del PR.
- Cada entrada dice qué vulnerabilidad es y su categoría del OWASP Top 10, dónde está (módulo y endpoint o componente), qué muestra en la demo y cómo se mitiga.
- Los pasos de explotación y los payloads de la demo no van acá ni en ningún archivo que carguen los agentes. Este archivo entra en el contexto de cada sesión, y ese contenido puede activar los filtros de seguridad del modelo y cortar la sesión.

| Paso | Vulnerabilidad | Categoría del OWASP Top 10 | Dónde | Qué muestra en la demo | Mitigación |
| --- | --- | --- | --- | --- | --- |
| 1 | Login con una consulta armada sin parámetros | Injection | Seguridad: el endpoint de login del monolito. | La verificación de credenciales arma la consulta con los datos del formulario, y eso permite entrar sin conocer la contraseña. Es el único punto del sistema con ese patrón. | Consulta parametrizada y validación server-side. |
| 2 | Sesión en una cookie base64 sin firma, con permisos que el back no vuelve a derivar | Broken Access Control | Seguridad: la emisión y la lectura de la cookie, y el gating de módulos. El selector de módulos del listado de Auditoría deja ver que existe Tesorería. | El back confía en los permisos que trae la cookie y, como no está firmada, se pueden cambiar para habilitar módulos que el usuario no tiene asignados. | Firmar la sesión o mantenerla server-side, y derivar los permisos a partir de la identidad. |
| 3 | El Diagnóstico de integraciones expone configuración sensible | Security Misconfiguration | Tesorería: la sección Diagnóstico. | La respuesta incluye la URL del módulo messaging y la credencial de su cuenta técnica de solo lectura. | No exponer secretos ni configuración interna en las respuestas. |
| 4 | Recuperación débil de la cuenta técnica y baja del evento de auditoría pendiente | Identification and Authentication Failures | El módulo messaging del monolito: la recuperación de la cuenta técnica y la baja de pendientes. La demora del consumer de Auditoría abre la ventana. | La recuperación no pide un segundo secreto ni verifica al solicitante, y la sesión que devuelve permite dar de baja un evento pendiente antes de que Auditoría lo registre. | Verificación fuerte de identidad y segundo factor en la recuperación, mínimo privilegio para la cuenta técnica, autorización estricta sobre los pendientes y un registro de auditoría append-only. |
