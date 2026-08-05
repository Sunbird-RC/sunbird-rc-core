# Deploying the multi-issuer portal

Four steps, run **on the deployment host** from the repo root. Every step is
idempotent, so a re-run after a failure continues rather than duplicating.

Why on the host: the registry's write API (`/api/v1`) is not published through the
gateway on this deployment — it answers `404` from nginx — and the new registry
entities arrive as files in a directory the registry container mounts. Neither is
reachable from outside the box.

## 0. What is being deployed

| Image | Tag |
| --- | --- |
| `pallakartheekreddy/sunbird-rc-oid4vc-service` | `issuers-1` |
| `pallakartheekreddy/sunbird-rc-oid4vc-issuer-portal` | `0.4.1` |

Both `linux/amd64`, already on Docker Hub. **These tags are the source of truth** —
prefer the inline-export command in step 2 over the `.env.issuers` file, which is a
convenience copy and can fall behind.

`0.4.1` supersedes `0.4.0`: same portal plus the redesigned sign-in screen.

> **The image and its environment must ship together.** `oid4vc-service` no longer
> hard-codes which registry entities a credential's claims come from — that is
> configuration now, which is what lets a new credential type be added without a
> code change. The consequence is that the service **requires**
> `REGISTRY_SUBJECT_ENTITY` and `REGISTRY_SUBJECT_KEY`; without them, wallet
> self-service issuance fails with *"registry claim sources are not configured"*.
> `docker-compose.issuers.yml` supplies them. Pulling the new image without that
> overlay breaks the login-gated flow that currently works.

## 1. Registry schemas

Three files add the `Issuer` and `Qualification` entities and `issuerId` on
`Farmer`. Get them onto the host (`git pull`, or copy) so they sit in the
`registry-schemas/` directory that `docker-compose.cloud.yml` already mounts:

```
registry-schemas/Issuer.json
registry-schemas/Qualification.json
registry-schemas/Farmer.json        # adds issuerId — existing records are unaffected
```

The registry reads that directory **once, at boot**, so it must restart:

```sh
docker compose -f docker-compose.cloud.yml restart registry
```

Java under emulation takes a few minutes. Wait for healthy:

```sh
docker compose -f docker-compose.cloud.yml ps registry
```

## 2. Services

```sh
OID4VC_FIX_ORG=pallakartheekreddy OID4VC_FIX_TAG=issuers-1 \
ISSUER_PORTAL_ORG=pallakartheekreddy ISSUER_PORTAL_TAG=0.4.1 \
docker compose -f docker-compose.cloud.yml -f docker-compose.demo.yml \
  -f docker-compose.authcode.yml -f docker-compose.authsrv.yml \
  -f docker-compose.issuers.yml \
  up -d oid4vc-service issuer-portal
```

**Every `-f` the deployment normally uses must be present, with
`docker-compose.issuers.yml` last.** It is not optional verbosity: that overlay
adds a `keycloak` fragment for the theme mount, and `docker-compose.cloud.yml`
does not define a `keycloak` service, so the short two-file form fails with

```
service "keycloak" has neither an image nor a build context specified
```

before anything is pulled. Verified by running `config` on the two-file form.

Exported inline rather than via `--env-file`: that flag *replaces* `.env` instead
of adding to it, so it would drop `POSTGRES_PASSWORD` and the other required
secrets and compose would refuse to start. Appending the four lines from
`.env.issuers` to the deployment's own `.env` works too.

## 3. Data and Keycloak

```sh
KC_ADMIN_PASSWORD='<realm admin password>' \
BASE=https://98.70.36.106.sslip.io \
  ./scripts/seed-issuers.sh
```

This creates the two new credential schemas (each with its own freshly minted
`did:web`), the three issuer records, the sample holders, and backfills `issuerId`
onto existing farmers so they stay visible. It **reuses** the existing farmer
credential rather than creating a second one.

`KC_ADMIN_PASSWORD` is what makes it register **one OAuth scope per credential
type**. That is not cosmetic: a wallet asks Keycloak for
`scope=<slug of the credential name>`, and a scope the client cannot request comes
back as an immediate redirect with `error=invalid_scope` — the holder never sees a
login page and the wallet reports only *"something went wrong"*. Skipping this
leaves the Age and Education credentials broken in exactly that way.

It also creates the sample logins `citizen.meera` (AGE-000001) and `student.priya`
(EDU-000001).

## 4. Verify

```sh
# three issuers, each with its own signing DID
curl -s https://98.70.36.106.sslip.io/credential-schema/oid4vci-configs \
  | python3 -c 'import json,sys; [print(c["name"], "|", c["author"]) for c in json.load(sys.stdin) if "vc+sd-jwt" in (c.get("formats") or [])]'

# logos are served by the portal image
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' \
  https://98.70.36.106.sslip.io/issuer-portal/logos/education.svg   # expect 200 image/svg+xml
```

Then sign in at `/issuer-portal/` as staff: the Issuers gallery should show three
authorities with logos and holder counts.

**End to end**, from the repo that holds the wallet harness:

```sh
cd demo-openid
npx vite-node src/__authcode_scan_check.ts '<offer from the portal>' student.priya '<password>'
```

Expect `ISSUED AFTER LOGIN`. Unlike localhost, this deployment is HTTPS, so the
wallet also *verifies* the credential — `did:web` resolution requires HTTPS, which
is why the same check can only confirm issuance locally.

## Rollback

```sh
docker compose -f docker-compose.cloud.yml -f docker-compose.demo.yml \
  -f docker-compose.authcode.yml -f docker-compose.authsrv.yml \
  up -d oid4vc-service issuer-portal
```

Dropping the one `-f docker-compose.issuers.yml` is the whole rollback — the other
files are the deployment's normal chain and are unchanged by any of this.
Restores the previously pinned tags. The added registry entities and seeded records
are harmless to leave in place — nothing else reads them — and the old portal
ignores `issuerId`.

## The sign-in theme

`keycloak-themes/sunbird-issuer` restyles the Keycloak login page to match the
portal. It is CSS-only over the stock `keycloak` parent theme — no FreeMarker
templates — because this deployment runs Keycloak 14 and those templates change
shape in 17+ and again in 22+.

Deploy:

```sh
# 1. the theme files, into a directory the keycloak container mounts
#    (docker-compose.issuers.yml adds the mount, read-only)
docker compose -f docker-compose.cloud.yml -f docker-compose.demo.yml \
  -f docker-compose.authcode.yml -f docker-compose.authsrv.yml \
  -f docker-compose.issuers.yml up -d keycloak

# 2. nginx caches upstream IPs at startup, and step 1 recreated the container
docker restart <project>-nginx-1

# 3. select it on the realm (also sets the brand lockup it styles)
LOGIN_THEME=sunbird-issuer KC_BASE=<gateway> \
  KC_ADMIN_PASSWORD=… node scripts/setup-keycloak-issuer.mjs
```

Two traps, both hit for real while deploying this:

**Editing a stylesheet in place does not take effect.** Keycloak's resource URL is
`/auth/resources/<hash>/login/<theme>/…`, and that hash comes from the SERVER build
— not from theme contents. Caches in the chain keep serving the old file under the
same URL, with `Cache-Control: max-age=2592000`. The stylesheet therefore carries a
version in its FILENAME (`sunbird-issuer-v2.css`, listed in `theme.properties`);
bump it on every change. Symptom if you forget: the served file contains your edit
(`curl` proves it) but no browser renders it — including incognito.

**Recreating Keycloak destroys a local realm.** The local compose runs it on H2
with no volume, so `up -d keycloak` wipes realm, clients and users. Re-run
`scripts/bootstrap-local.sh` (or the setup script) afterwards to rebuild them. The
server uses Postgres and is unaffected.
