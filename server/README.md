# Spectacled CORS Proxy

A tiny [Ktor](https://ktor.io/) reverse proxy that lets the **web build** of Spectacled talk
to CalDAV servers.

## Why this exists

Spectacled talks to CalDAV servers using WebDAV HTTP methods (`PROPFIND`, `REPORT`, `MKCOL`, …).
On Android, iOS, and Desktop the app contacts your CalDAV server directly. **In the browser it
can't:** browsers enforce [CORS](https://developer.mozilla.org/docs/Web/HTTP/CORS), and CalDAV
servers (Nextcloud, Radicale, …) generally don't send the CORS headers a browser requires for
cross-origin WebDAV. Since Spectacled works against *any* server you point it at, we can't rely on
each of those servers being reconfigured.

This proxy sits between the web app and your CalDAV server: the browser calls the proxy (same
origin policy satisfied by CORS headers the proxy adds), and the proxy forwards the request to the
real server named in the `X-Target-Url` header. CORS is a **browser-only** restriction — the native
apps don't use this proxy at all.

> ### ⚠️ Trust: run your own
> The proxy terminates TLS, so it sees the `Authorization` header (your CalDAV credentials) in
> transit. **Whoever runs the proxy could read those credentials.** For that reason:
> - **Self-host your own instance** whenever you can — then you are the only one in the path.
> - Any shared/public instance (including a project demo) should be treated as **evaluation only —
>   do not use real credentials** against a proxy you don't control.

## How it works

- Reads the destination from the `X-Target-Url` request header (or a `?target=` query parameter).
- Validates the target (scheme, host allow-list, private-address block — see below), then forwards
  the method, headers, and body, streaming the response back with permissive CORS headers.
- `GET /` is a health/info endpoint.

## Configuration (environment variables)

| Variable                     | Default  | Purpose                                                                                       |
|------------------------------|----------|-----------------------------------------------------------------------------------------------|
| `PORT`                       | `8088`   | Port to bind. PaaS hosts (Fly.io, Render, …) inject this automatically.                        |
| `PROXY_ALLOWED_ORIGINS`      | *(any)*  | Comma-separated web origins allowed by CORS, e.g. `https://spectacled.techbee.at`. Unset = reflect any origin (**dev only**, logged as a warning). Set this in production. |
| `PROXY_ALLOWED_TARGET_HOSTS` | *(any)*  | Comma-separated allow-list of destination hostnames. Unset = any host. Strongly recommended for a shared/demo instance so it can't be abused as an open relay. |
| `PROXY_REQUIRE_HTTPS_TARGET` | `true`   | Reject non-`https` target URLs.                                                               |
| `PROXY_ALLOW_PRIVATE_TARGETS`| `false`  | When `false`, targets that resolve to loopback/link-local/private/unique-local addresses (e.g. `169.254.169.254`, `127.0.0.1`) are rejected. This is the SSRF guard — leave it off in production. |

## Run locally

```bash
# From the repository root:
./gradlew :server:run
# Proxy on http://localhost:8088 (allows any origin/target — dev defaults)

# In the web app's Settings → Proxy server, use: http://localhost:8088
```

## Build a container

The image builds from the **repository root** (the module depends on `:shared`):

```bash
docker build -f server/Dockerfile -t spectacled-proxy .
docker run --rm -p 8088:8080 \
  -e PROXY_ALLOWED_ORIGINS=https://spectacled.techbee.at \
  -e PROXY_ALLOWED_TARGET_HOSTS=your-caldav.example \
  spectacled-proxy
```

Rather than repeating `-e` flags, copy [`.env.example`](.env.example) to `.env`, edit it,
and pass it at run time (the config is read when the container starts, not baked into the
image; `.env` is gitignored):

```bash
cp server/.env.example server/.env   # then edit server/.env
docker run --rm -p 8088:8080 --env-file server/.env spectacled-proxy
```

A pre-built image is published to GitHub Container Registry on `proxy-v*` tags
(see `.github/workflows/publish-proxy-image.yml`):

```bash
docker run --rm -p 8088:8080 \
  -e PROXY_ALLOWED_ORIGINS=https://your-web-app.example \
  ghcr.io/techbeeat/spectacled-proxy:latest
```

## Deploy to Fly.io (recommended)

[`fly.toml`](../fly.toml) in the repo root is a template. From the repository root:

```bash
fly launch --copy-config --no-deploy   # or: fly apps create <your-app-name>
# Edit fly.toml: set a unique `app` name and your PROXY_ALLOWED_ORIGINS
fly deploy
```

Fly builds `server/Dockerfile`, injects `PORT`, and terminates TLS for you. The template scales to
zero (`auto_stop_machines`) to keep idle cost near nothing; expect a brief cold start on the first
request after idle. A `shared-cpu-1x` / 512 MB machine is enough for this JVM app.

Other container hosts (Render, Koyeb, Railway, …) work the same way — point them at
`server/Dockerfile`, set `PROXY_ALLOWED_ORIGINS`, and let the platform provide `PORT`.

## Tests

```bash
./gradlew :server:test
```

Covers the health endpoint, missing/rejected targets (host allow-list, https-only, private-address
SSRF guard), CORS preflight, and a full proxied round-trip.
