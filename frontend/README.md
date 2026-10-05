# Frontend

React 18 + Vite app (`frontend/`) for the SGM, per [TPS-26](https://frba-team-zh8igc5a.atlassian.net/browse/TPS-26). JavaScript only, no TypeScript.

This is the skeleton: the routing conventions, the proxy to the two backends and a status screen. The business screens land with their own tickets (login in TPS-14, module gating in TPS-15, VEP in TPS-21, and one ticket per module).

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

The targets come from `MONOLITH_URL` and `AUDITORIA_URL` (see `vite.config.js`); the compose sets them to `http://monolith:8080` and `http://auditoria:8081`. In the code, call the backends with relative paths (`fetch('/api/...')`, or `getJson` from `src/api/http.js`).

## Routes

Routes are declared as arrays in `src/routes/public.js` (no session) and `src/routes/private.js` (session required; `code` is the permission that enables the route), and `src/Router.jsx` builds the `<Route>` tree from them. Views live in `src/views/<Name>View/index.jsx` and are exported from `src/views/index.js`.

| Route | Access | Screen |
| --- | --- | --- |
| `/` | public | Redirects to `/estado`. |
| `/estado` | public | Status of both backends (`StatusView`). |
| `/vep` | public | Placeholder for the public debt lookup (TPS-21). |
| `/login` | public | Placeholder for the login form (TPS-14). |
| `/unauthorized` | public | Expired session or no access. |
| `*` | public | Not found. |
| `/welcome` | behind the login | Module selector after the login (`WelcomeView`); TPS-15 fills it. |
| `/administracion/**`, `/tesoreria/**`, `/auditoria/**` | behind the login | Module screens; each module ticket adds them to `private.js` with its permission `code`. |

Naming: routes are in Spanish, kebab-case, prefixed with the module (`/tesoreria/...`); lists in plural (`/tesoreria/deudas`) and details in singular with a parameter (`/tesoreria/deuda/:cuit`). The generic routes are `/login`, `/welcome` and `/unauthorized`.

`PrivateRoute` (`src/components/common/PrivateRoute.jsx`) redirects to `/login` when `useSession()` returns no session. `useSession` (`src/hooks/useSession.js`) is a stub that returns `null` until TPS-13 defines the session cookie and TPS-14 issues it, so `/welcome` always redirects for now.

## Adding a screen

1. Create the view in `src/views/<Name>View/index.jsx` and export it from `src/views/index.js`.
2. Add an entry to `src/routes/private.js` (or `public.js` for a public screen) with `path`, `title`, `component` and, for private routes, the permission `code`.
3. Call the backend with relative paths through the proxy; the session cookie travels on its own.

## Building

```bash
npm run build
```

Writes the static bundle to `dist/` (ignored by git). The CI runs `npm ci` and `npm run build` on every PR.

## Docker

The `frontend` service in the compose builds `frontend/Dockerfile` (Node 22) and runs the Vite dev server with the code copied into the image, like the backends: changes need `docker compose up -d --build`. The port is published only on `127.0.0.1:5173`.
