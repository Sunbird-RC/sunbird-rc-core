#!/usr/bin/env bash
# Seeds the multi-issuer demo data into a RUNNING Sunbird RC stack.
#
# Idempotent and safe to re-run: every step checks before it writes, so a partly
# seeded deployment is completed rather than duplicated.
#
#   BASE=https://98.70.36.106.sslip.io ./scripts/seed-issuers.sh
#   BASE=http://localhost              ./scripts/seed-issuers.sh   # default
#
# Talks only to the gateway, so it runs from anywhere the gateway is reachable —
# EXCEPT that the registry write API (/api/v1) is not published on every
# deployment. Where it isn't, this must run on the host. The preflight says which
# situation you are in rather than failing halfway through.
#
# What it does NOT do: pull images, restart containers, or install registry
# schemas. Those are deployment steps — see docker-compose.issuers.yml. This
# creates data only.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE="${BASE:-http://localhost}"
BASE="${BASE%/}"

# The registry's write API is often NOT published through the gateway — on the
# reference deployment nginx answers 404 for /api/v1 while the registry itself
# listens on :8091. Kept separate so the script can use the public gateway for
# credential-schema and identity (which need the public hostname to appear in DIDs
# and vct URLs) while talking to the registry directly.
REG="${REGISTRY_BASE:-$BASE}"
REG="${REG%/}"

# Keycloak realm setup needs admin credentials, and re-running it is how the
# per-credential-type OAuth scopes get created. Skipped unless a password is given.
KC_ADMIN="${KC_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:-}"
KC_REALM="${KC_REALM:-sunbird-rc}"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
info() { printf '    %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*"; }
die()  { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; exit 1; }

# --- 0. preflight ------------------------------------------------------------
say "0. Preflight — gateway $BASE, registry $REG"

curl -fsS -o /dev/null --max-time 15 "$BASE/health" >/dev/null 2>&1 \
  && ok "gateway answering" \
  || die "gateway not reachable at $BASE/health"

curl -fsS -o /dev/null --max-time 20 "$BASE/credential-schema/oid4vci-configs" >/dev/null 2>&1 \
  && ok "credential-schema reachable" \
  || die "credential-schema not reachable through $BASE"

# The registry write API is the surface most often left unpublished. A real search
# is the only reliable probe: a 404 here is the gateway, not the registry, and no
# amount of retrying will change it.
REG_PROBE="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 \
  -X POST "$REG/api/v1/Farmer/search" -H 'content-type: application/json' \
  -d '{"offset":0,"limit":1,"filters":{}}' 2>/dev/null || echo 000)"
case "$REG_PROBE" in
  200|201) ok "registry write API reachable" ;;
  404) die "the registry API is not published at $REG/api/v1 (gateway returned 404).
    Point REGISTRY_BASE at the registry directly — on the reference deployment
    that is http://localhost:8091, reachable only from the host:
      BASE=$BASE REGISTRY_BASE=http://localhost:8091 $0" ;;
  401|403) die "the registry API requires authentication ($REG_PROBE).
    Run this on the host, or set authentication_enabled=false." ;;
  *) die "unexpected response from the registry API: $REG_PROBE" ;;
esac

# The Issuer entity is what everything below attaches to. It exists only once the
# schema files are in registry-schemas/ AND the registry has been restarted.
ISSUER_PROBE="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 \
  -X POST "$REG/api/v1/Issuer/search" -H 'content-type: application/json' \
  -d '{"offset":0,"limit":1,"filters":{}}' 2>/dev/null || echo 000)"
if [ "$ISSUER_PROBE" != "200" ] && [ "$ISSUER_PROBE" != "201" ]; then
  die "the registry has no Issuer entity yet (got $ISSUER_PROBE).
    Copy registry-schemas/{Issuer,Qualification,Farmer}.json onto the host's
    registry-schemas/ directory, then restart the registry:
      docker compose -f docker-compose.cloud.yml restart registry
    The registry reads that directory once, at boot."
fi
ok "Issuer and Qualification entities present"

# --- helpers -----------------------------------------------------------------
reg_post() {
  curl -fsS -X POST "$REG/api/v1/$1" -H 'content-type: application/json' -d "$2" >/dev/null 2>&1
}
reg_search() {
  curl -fsS -X POST "$REG/api/v1/$1/search" -H 'content-type: application/json' \
    -d '{"offset":0,"limit":1000,"filters":{}}' 2>/dev/null
}
reg_count() {
  reg_search "$1" | python3 -c 'import json,sys
try:
    d=json.load(sys.stdin); print(len(d if isinstance(d,list) else d.get("data",[])))
except Exception: print(0)'
}

# --- 1. credential schemas, each with its own did:web ------------------------
#
# Each issuer signs with its OWN DID, because an issuer in VC terms IS a DID: a
# credential's `issuer` is what a verifier checks, so authorities sharing one key
# would be indistinguishable to any wallet. oid4vc-service signs with the `author`
# of the bound schema, so a DID per schema is all that is needed.
#
# A DID is minted ONLY when a schema has to be created. Where the schema already
# exists its author already IS that authority's DID, and minting another would
# leave an unused key in the vault and a DID document nothing ever signs with.
say "1. Credential schemas and signing DIDs"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Helper scripts live in files rather than heredocs inside $(...): bash 3.2 — still
# the default on macOS — cannot parse that combination inside a function body and
# fails with an unhelpful "unexpected EOF".
cat > "$WORK/schema.py" <<'PYEOF'
import json, sys
name, sid, author, slug, props, required, desc, base = sys.argv[1:9]
print(json.dumps({
    "schema": {
        "type": "https://w3c-ccg.github.io/vc-json-schemas/",
        "version": "1.0.0",
        "id": sid,
        "name": name,
        "author": author,
        "authored": "2026-01-01T00:00:00.000Z",
        "schema": {
            "$id": sid,
            "$schema": "https://json-schema.org/draft/2019-09/schema",
            "description": desc,
            "type": "object",
            "properties": json.loads(props),
            "required": json.loads(required),
            # MUST be true. Issuance always adds credentialSubject.id (the holder
            # DID); with this false, credentials-service rejects every issuance
            # with a must-NOT-have-additional-properties error that reaches the
            # wallet only as an opaque 500.
            "additionalProperties": True,
        },
    },
    "tags": [slug],
    # PUBLISHED is required: getOid4vciConfigs only looks at published schemas.
    "status": "PUBLISHED",
    "oid4vciConfig": {
        "oid4vciEnabled": True,
        "oid4vciFormats": ["vc+sd-jwt"],
        "vct": base + "/vct/" + slug,
        # `locale` is REQUIRED on a display entry. Without it a wallet fetching the
        # SD-JWT VC Type Metadata fails to parse it and shows only "something went
        # wrong", with no clue that the cause is here.
        "display": [{"name": name, "locale": "en-US"}],
    },
}))
PYEOF

cat > "$WORK/find.py" <<'PYEOF'
import json, sys
want = sys.argv[1].lower()
for c in json.load(sys.stdin):
    if "vc+sd-jwt" not in (c.get("formats") or []):
        continue
    if want in (c.get("name") or "").lower():
        print(c.get("schemaId", ""))
        break
PYEOF

cat > "$WORK/author.py" <<'PYEOF'
import json, sys
want = sys.argv[1]
for c in json.load(sys.stdin):
    if c.get("schemaId") == want:
        print(c.get("author") or "")
        break
PYEOF

cat > "$WORK/did.py" <<'PYEOF'
import json, sys
d = json.load(sys.stdin)
print((d[0] if isinstance(d, list) else d)["id"])
PYEOF

configs()     { curl -fsS "$BASE/credential-schema/oid4vci-configs" 2>/dev/null; }
find_schema() { configs | python3 "$WORK/find.py" "$1"; }
find_author() { configs | python3 "$WORK/author.py" "$1"; }

mint_did() {
  local label="$1" did_json did
  # `services` must be a non-empty array whose serviceEndpoint is a STRING;
  # identity-service rejects the document otherwise, and its error names the field
  # but not the reason.
  did_json="$(curl -fsS -X POST "$BASE/did/generate" -H 'content-type: application/json' \
    -d "{\"content\":[{\"alsoKnownAs\":[\"$label\"],\"method\":\"web\",\"services\":[{\"id\":\"IssuerService\",\"type\":\"LinkedDomains\",\"serviceEndpoint\":\"$BASE\"}]}]}" 2>/dev/null)" \
    || die "could not reach $BASE/did/generate — is identity-service published?"
  did="$(printf '%s' "$did_json" | python3 "$WORK/did.py")"
  [ -n "$did" ] || die "could not mint a DID for $label"
  printf '%s' "$did"
}

# Finds a schema by name, or mints a DID and creates it. Sets SCHEMA_ID and
# SCHEMA_DID for the caller: bash 3.2 has no name references, and returning two
# values on stdout would collide with the progress output.
ensure_schema() {
  local match="$1" name="$2" sid="$3" label="$4" slug="$5" props="$6" required="$7" desc="$8"
  local body resp
  SCHEMA_ID="$(find_schema "$match")"
  if [ -n "$SCHEMA_ID" ]; then
    SCHEMA_DID="$(find_author "$SCHEMA_ID")"
    ok "$name already present"
    info "$SCHEMA_ID"
    [ -n "$SCHEMA_DID" ] && info "signed by $SCHEMA_DID"
    return 0
  fi
  SCHEMA_DID="$(mint_did "$label")"
  ok "minted $SCHEMA_DID ($label)"
  body="$(python3 "$WORK/schema.py" "$name" "$sid" "$SCHEMA_DID" "$slug" "$props" "$required" "$desc" "$BASE")"
  resp="$(curl -fsS -X POST "$BASE/credential-schema" -H 'content-type: application/json' -d "$body" 2>&1)" \
    || die "creating $name failed: $resp"
  SCHEMA_ID="$(printf '%s' "$resp" | python3 -c 'import json,sys; print(json.load(sys.stdin)["schema"]["id"])' 2>/dev/null)"
  [ -n "$SCHEMA_ID" ] || die "no schema id returned for $name"
  ok "created $name"
  info "$SCHEMA_ID"
}

# The farmer credential is REUSED, never recreated: this deployment already has
# one, and a second would leave two authorities claiming the same credential.
FARMER_CONFIG_ID="$(find_schema 'farmer land holding')"
FARMER_DID=""
if [ -n "$FARMER_CONFIG_ID" ]; then
  FARMER_DID="$(find_author "$FARMER_CONFIG_ID")"
  ok "reusing the existing farmer credential"
  info "$FARMER_CONFIG_ID"
  [ -n "$FARMER_DID" ] && info "signed by $FARMER_DID"
else
  warn "no farmer SD-JWT credential found; that issuer will be created unbound"
fi

# Property names deliberately MATCH the registry field names wherever a standard
# does not dictate otherwise: claims resolve by name, so a well-named schema needs
# no alias at all. `birthdate` is the exception, and it is aliased in
# docker-compose.issuers.yml rather than in any service's code.
ensure_schema 'mobile age' 'Mobile Age Credential' 'MobileAgeCredential' \
  'civil-registration-authority' 'mobile-age-credential' \
  '{"name":{"type":"string","description":"Full name"},
    "birthdate":{"type":"string","description":"Date of birth, ISO 8601"},
    "age_over_18":{"type":"boolean","description":"Derived from the date of birth at issuance"}}' \
  '["name","birthdate"]' \
  'Proof of age, issued from the civil register.'
AGE_CONFIG_ID="$SCHEMA_ID"; AGE_DID="$SCHEMA_DID"

ensure_schema 'education certificate' 'Education Certificate Credential' \
  'EducationCertificateCredential' 'education-board' 'education-certificate-credential' \
  '{"name":{"type":"string","description":"Full name of the student"},
    "degree":{"type":"string","description":"Award as it appears on the certificate"},
    "institution":{"type":"string","description":"Awarding college, university or board"},
    "yearOfPassing":{"type":"number","description":"Year the award was conferred"},
    "grade":{"type":"string","description":"Grade, class or percentage"}}' \
  '["name","degree","institution"]' \
  'Qualification awarded to a student, issued from the education register.'
EDU_CONFIG_ID="$SCHEMA_ID"; EDU_DID="$SCHEMA_DID"

# --- 2. issuers --------------------------------------------------------------
say "2. Issuers"

# printf, not python: bash 3.2 mangles a multi-line `python3 -c "..."` nested in
# "$( )", silently dropping closing braces and reporting a syntax error that points
# at a line which looks fine.
ISSUER_FMT='{"issuerId":"%s","name":"%s","description":"%s","category":"%s","did":"%s","credentialConfigId":"%s","credentialName":"%s","holderLabel":"%s","holderIdPrefix":"%s","recordEntities":"%s","icon":"%s","accent":"%s","logoUrl":"%s","url":"%s","status":"Active"}'

EXISTING_ISSUERS="$(reg_search Issuer | python3 -c '
import json, sys
try: d = json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d, list) else d.get("data", [])
print(" ".join(r.get("issuerId","") for r in rows))')"

seed_issuer() {
  local iid="$1"; shift
  case " $EXISTING_ISSUERS " in
    *" $iid "*) info "$iid already present"; return 0 ;;
  esac
  reg_post Issuer "$(printf "$ISSUER_FMT" "$iid" "$@")" && ok "$iid" || warn "$iid failed"
}

seed_issuer 'ISS-FARMER' 'Department of Agriculture' \
  'Issues land-holding credentials to registered farmers, from the land and crop records held in the registry.' \
  'Agriculture' "$FARMER_DID" "$FARMER_CONFIG_ID" 'Farmer Land Holding Credential' \
  'Farmer' 'FRM' 'LandParcel,Crop,SeedDistribution' '🌾' '#4a7c59' \
  '/issuer-portal/logos/agriculture.svg' 'https://agricoop.gov.in'

seed_issuer 'ISS-AGE' 'Civil Registration Authority' \
  'Issues a minimal age credential — the holder proves they are over 18 without revealing anything else.' \
  'Identity' "$AGE_DID" "$AGE_CONFIG_ID" 'Mobile Age Credential' \
  'Citizen' 'AGE' '' '🪪' '#3d6b8c' \
  '/issuer-portal/logos/civil-registration.svg' 'https://crsorgi.gov.in'

seed_issuer 'ISS-EDUCATION' 'State Board of Education' \
  'Issues qualification credentials to students from the awards on their record.' \
  'Education' "$EDU_DID" "$EDU_CONFIG_ID" 'Education Certificate Credential' \
  'Student' 'EDU' 'Qualification' '🎓' '#8c5a3d' \
  '/issuer-portal/logos/education.svg' 'https://education.gov.in'

# --- 3. holders --------------------------------------------------------------
say "3. Holders"

ALL_IDS="$(reg_search Farmer | python3 -c '
import json, sys
try: d = json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d, list) else d.get("data", [])
print(" ".join(r.get("farmerId","") for r in rows))')"

seed_holder() {
  local fid="$1" body="$2" label="$3"
  case " $ALL_IDS " in
    *" $fid "*) info "$fid already present"; return 0 ;;
  esac
  reg_post Farmer "$body" && ok "$label" || warn "$fid failed"
}

seed_holder AGE-000001 '{"issuerId":"ISS-AGE","farmerId":"AGE-000001","name":"Meera Nair","gender":"Female","dateOfBirth":"2001-03-19","district":"Ernakulam","state":"Kerala","keycloakUsername":"citizen.meera"}' \
  'AGE-000001 (Meera Nair, adult)'
# A minor on purpose: age_over_18 resolves to false, which must still be a claim
# rather than a missing value.
seed_holder AGE-000002 '{"issuerId":"ISS-AGE","farmerId":"AGE-000002","name":"Arjun Das","gender":"Male","dateOfBirth":"2012-08-30","district":"Kozhikode","state":"Kerala"}' \
  'AGE-000002 (Arjun Das, minor — age_over_18 false)'
seed_holder EDU-000001 '{"issuerId":"ISS-EDUCATION","farmerId":"EDU-000001","name":"Priya Sharma","gender":"Female","dateOfBirth":"1998-06-05","district":"Bengaluru Urban","state":"Karnataka","keycloakUsername":"student.priya"}' \
  'EDU-000001 (Priya Sharma)'
# No qualification on purpose: the Education issuer's blocked-issuance case.
seed_holder EDU-000002 '{"issuerId":"ISS-EDUCATION","farmerId":"EDU-000002","name":"Rahul Verma","gender":"Male","district":"Mysuru","state":"Karnataka"}' \
  'EDU-000002 (no award — negative case)'

if [ "$(reg_count Qualification)" -gt 0 ]; then
  info "$(reg_count Qualification) qualification(s) already present"
else
  reg_post Qualification '{"farmerId":"EDU-000001","degree":"B.Sc. Agriculture","institution":"University of Agricultural Sciences, Bengaluru","yearOfPassing":2020,"grade":"First Class","enrolmentNumber":"UAS-2016-4471"}' \
    && ok "Qualification B.Sc. Agriculture 2020" || warn "qualification 1 failed"
  # An earlier award, so "most recent wins" is observable in the claim table.
  reg_post Qualification '{"farmerId":"EDU-000001","degree":"Higher Secondary","institution":"Karnataka State Board","yearOfPassing":2016,"grade":"88%"}' \
    && ok "Qualification Higher Secondary 2016 (older — must NOT be the one issued)" || warn "qualification 2 failed"
fi

# --- 4. backfill -------------------------------------------------------------
# Holders created before issuers existed carry no issuerId, so no issuer's list
# would show them and the portal would appear to have lost them.
say "4. Backfilling holders with no issuer"
UNASSIGNED="$(reg_search Farmer | python3 -c '
import json, sys
try: d = json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d, list) else d.get("data", [])
for r in rows:
    if not r.get("issuerId"):
        print(json.dumps(r))')"
if [ -z "$UNASSIGNED" ]; then
  ok "every holder already has an issuer"
else
  printf '%s\n' "$UNASSIGNED" | while IFS= read -r row; do
    [ -n "$row" ] || continue
    OSID="$(printf '%s' "$row" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("osid",""))')"
    FID="$(printf '%s' "$row" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("farmerId",""))')"
    # Guessed from the id prefix, defaulting to the farmer issuer: records that
    # predate the issuer concept were all farmers on this deployment.
    case "$FID" in
      AGE-*) TARGET=ISS-AGE ;;
      EDU-*) TARGET=ISS-EDUCATION ;;
      *)     TARGET=ISS-FARMER ;;
    esac
    BODY="$(printf '%s' "$row" | python3 -c '
import json, sys
r = json.load(sys.stdin)
r["issuerId"] = sys.argv[1]
r.pop("osid", None)
print(json.dumps(r))' "$TARGET")"
    curl -fsS -X PUT "$REG/api/v1/Farmer/$OSID" -H 'content-type: application/json' \
      -d "$BODY" >/dev/null 2>&1 && ok "$FID -> $TARGET" || warn "could not backfill $FID"
  done
fi

# --- 5. Keycloak -------------------------------------------------------------
# A scope per credential type is REQUIRED, not cosmetic: a wallet running
# authorization_code asks Keycloak for `scope=<slug of the credential name>`, and a
# scope the client cannot request comes back as an immediate redirect carrying
# error=invalid_scope — so the holder never sees a login page and the wallet
# reports only "something went wrong".
say "5. Keycloak realm, credential scopes and sample logins"
if ! command -v node >/dev/null 2>&1; then
  warn "node is not installed here — skipping the Keycloak step."
  info "Run it from a machine that has node and can reach $BASE:"
  info "  KC_BASE=$BASE KC_ADMIN_PASSWORD=… node scripts/setup-keycloak-issuer.mjs"
  info "Until then the Age and Education credentials fail wallet sign-in with"
  info "invalid_scope, because no OAuth scope exists for them."
elif [ -z "$KC_ADMIN_PASSWORD" ]; then
  warn "KC_ADMIN_PASSWORD not set — skipped."
  info "The Age and Education credentials will fail wallet sign-in with"
  info "invalid_scope until this runs. Re-run with:"
  info "  KC_ADMIN_PASSWORD=… BASE=$BASE ./scripts/seed-issuers.sh"
else
  ( cd "$ROOT" && \
    KC_BASE="$BASE" KC_ADMIN="$KC_ADMIN" KC_ADMIN_PASSWORD="$KC_ADMIN_PASSWORD" \
    KC_REALM="$KC_REALM" PORTAL_PUBLIC_URL="$BASE" PORTAL_BASE_PATH=/issuer-portal \
    ${ISSUER_PORTAL_CLIENT_SECRET:+ISSUER_PORTAL_CLIENT_SECRET="$ISSUER_PORTAL_CLIENT_SECRET"} \
    node scripts/setup-keycloak-issuer.mjs ) | sed 's/^/  /'
fi

# --- summary -----------------------------------------------------------------
say "Seeded"
cat <<EOF
  Issuer portal   $BASE/issuer-portal/
  Verifier        $BASE/verifier-app/

  Issuers         $(reg_count Issuer)
  Holders         $(reg_count Farmer)

  Sample holders:
    citizen.meera  -> AGE-000001  adult, age_over_18 true
    student.priya  -> EDU-000001  two awards; the newest is the one issued
                      EDU-000002  no award — issuance is BLOCKED, by design
EOF
