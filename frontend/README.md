# Frontend

React 18 + Vite app (`frontend/`) for the SGM, per [TPS-26](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-26). JavaScript only, no TypeScript.

Besides the skeleton (the routing, the proxy to the two backends and a status screen), it has the common pieces from [TPS-27](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-27): the session (`src/context/SessionProvider.jsx`), the HTTP client (`src/api/http.js`), the styles (`src/styles/theme.css`, Bootstrap with the SGM palette) and the shared components in `src/components/common/`, each one documented in a comment at the top of its file. The code conventions are in [`AGENTS.md`](../AGENTS.md#convenciones-de-código). The business screens land with their own tickets (login in TPS-14, module gating in TPS-15, VEP in TPS-21, and one ticket per module).

## Prerequisites

- **Node.js 22** (22.12 or newer, which Vite 7 requires) and npm. Or just Docker, to run it from the repo's `docker-compose.yml`.

## Running locally

From `frontend/`:

```bash
npm install
npm run dev
```

The dev server listens on http://localhost:5173 (`strictPort`: if the port is taken it fails instead of picking another one). It needs the backends running, from the compose (`docker compose up -d --build` at the repo root) or from your IDE.

## The proxy

The browser only talks to the front. Vite's dev server proxies the API calls, so the session cookie reaches both backends from a single origin, without CORS:

| Path in the browser | Goes to | Default target |
| --- | --- | --- |
| `/api/**` | Monolith (also `/api/messaging/**`, with its own `Authorization: Bearer` header, forwarded unchanged) | `http://localhost:8080` |
| `/auditoria/api/**` | Auditoría, with the `/auditoria` prefix removed | `http://localhost:8081` |

The targets come from `MONOLITH_URL` and `AUDITORIA_URL` (see `vite.config.js`); the compose sets them to `http://monolith:8080` and `http://auditoria:8081`. In the code, call the backends with relative paths through `src/api/http.js` (`getJson('/api/...')`).

## Routes

Routes are declared as arrays in `src/routes/public.js` (no session) and `src/routes/private.js` (session required; `code` is the permission that enables the route), and `src/Router.jsx` builds the `<Route>` tree from them. Views live in `src/views/<Name>View/index.jsx` and are exported from `src/views/index.js`. The generic routes:

| Route | Access | Screen |
| --- | --- | --- |
| `/` | public | Redirects to `/estado`. |
| `/estado` | public | Status of both backends (`StatusView`). |
| `/vep` | public | Public debt lookup (TPS-21). |
| `/login` | public | Login form (TPS-14). |
| `/unauthorized` | public | Expired session or no access. |
| `*` | public | Not found. |
| `/welcome` | behind the login | Module selector: one button per module enabled in the session (`WelcomeView`). |

Each module's routes are in `src/routes/private.js`, with the module prefix (`/administracion`, `/tesoreria`, `/auditoria`) and its permission `code`; the module is entered through the screen listed in `src/routes/modules.js`. Ingresos Públicos and Contaduría are decorative modules, so the system looks complete: they have a permission and show up in `/welcome` like the others, but all their routes are `placeholder: true`. Route flags: `layout: 'minimal'` (header with only the brand and the session), `menu: true` (shows up in the header menu, which lists the public routes when there is no session and, inside a module, only that module's routes; the modules themselves are chosen in `/welcome`, never in the header), `guestOnly: true` (a public route only for visitors without a session, like `/login`) and `placeholder: true` (a decorative menu option, without `component`, that opens "Sección fuera de la demo").

The session comes from `SessionProvider` (`src/context/SessionProvider.jsx`), which asks `GET /api/auth/session` when the app loads: a `200` is a session, anything else (`401` without a valid `sgm_session` cookie) is none. Until TPS-14 ships the real login, the only way to get a session in the dev server is to set the `sgm_session` cookie by hand (e.g. from the browser's DevTools — it is `HttpOnly`, so `document.cookie` cannot set it). `src/api/http.js` handles the auth errors: a `401` clears the session and goes to `/login`, remembering the destination, and a `403` goes to `/unauthorized`.

## Adding a screen

1. Create the view in `src/views/<Name>View/index.jsx` and export it from `src/views/index.js`. `StatusView` is the example of a list screen.
2. Add an entry to `src/routes/private.js` (or `public.js` for a public screen) with `path`, `title`, `component`, the permission `code` for private routes, and the flags it needs.
3. For a new module, add it to `src/routes/modules.js`.
4. Call the backend through `src/api/http.js` (`getJson`, `postJson`, …) with relative paths; the session cookie travels on its own.

## Building

```bash
npm run build
```

Writes the static bundle to `dist/` (ignored by git). The CI runs `npm ci` and `npm run build` on every PR.

## Docker

The `frontend` service in the compose builds `frontend/Dockerfile` (Node 22) and runs the Vite dev server with the code copied into the image, like the backends: changes need `docker compose up -d --build`. The port is published only on `127.0.0.1:5173`.
