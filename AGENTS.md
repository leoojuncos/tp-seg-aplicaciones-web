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
