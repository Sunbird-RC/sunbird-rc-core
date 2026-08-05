#!/usr/bin/env bash
# Prepares the localhost stack so the issuer portal can be used end to end.
#
# Idempotent: safe to re-run after `docker compose restart`. Each step reports
# what it found or created.
#
#   docker compose -f docker-compose.local.yml up -d
#   ./scripts/bootstrap-local.sh
#   open http://localhost/issuer-portal/
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE=(docker compose -f "$ROOT/docker-compose.local.yml")
BASE=http://localhost
KC="$BASE/auth"
REALM=sunbird-rc
KC_ADMIN=admin
KC_ADMIN_PASSWORD=admin123
PORTAL_SECRET=local-portal-secret

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
info() { printf '    %s\n' "$*"; }
die()  { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; exit 1; }

wait_for() {
  local label="$1" url="$2" tries="${3:-60}"
  printf '  waiting for %s' "$label"
  for _ in $(seq 1 "$tries"); do
    if curl -fsS -o /dev/null --max-time 5 "$url" 2>/dev/null; then printf ' ok\n'; return 0; fi
    printf '.'; sleep 5
  done
  printf '\n'; die "$label did not become ready: $url"
}

# --- 1. wait for the stack ---------------------------------------------------
say "1. Waiting for services (amd64 emulation makes first boot slow)"
wait_for "nginx"             "$BASE/health"                            60
wait_for "keycloak"          "$KC/realms/master"                       90
# A real health endpoint, not a DID resolution: resolving a made-up DID returns
# 404, so it never reports ready and `|| true` cannot rescue it — `die` exits the
# shell regardless of the calling context.
wait_for "identity"          "$BASE/identity-health"                   60
wait_for "credential"        "$BASE/credential-health"                 60
wait_for "credential-schema" "$BASE/credential-schema/oid4vci-configs" 60
wait_for "registry"          "$BASE/registry/health"                   90
ok "all gateways answering"

# --- 2. Vault kv engine ------------------------------------------------------
# identity-service is configured with VAULT_ROOT_PATH=kv, but dev-mode Vault
# only mounts kv-v2 at `secret/`. Without this, every signing key write fails.
say "2. Vault kv-v2 engine at kv/"
if "${COMPOSE[@]}" exec -T vault sh -c \
    'VAULT_TOKEN=local-root-token vault secrets list -format=json 2>/dev/null | grep -q "\"kv/\""'; then
  ok "kv/ already mounted"
else
  "${COMPOSE[@]}" exec -T vault sh -c \
    'VAULT_TOKEN=local-root-token vault secrets enable -path=kv -version=2 kv' >/dev/null \
    && ok "kv/ mounted (kv-v2)" || die "could not mount kv/"
fi

# --- 3. Keycloak realm, clients, roles, users -------------------------------
say "3. Keycloak realm and clients"
( cd "$ROOT" && \
  KC_BASE="$BASE" KC_ADMIN="$KC_ADMIN" KC_ADMIN_PASSWORD="$KC_ADMIN_PASSWORD" \
  KC_REALM="$REALM" PORTAL_PUBLIC_URL="$BASE" PORTAL_BASE_PATH=/issuer-portal \
  ISSUER_PORTAL_CLIENT_SECRET="$PORTAL_SECRET" \
  node scripts/setup-keycloak-issuer.mjs ) | sed 's/^/  /'


# --- 4. one did:web per issuing authority ------------------------------------
#
# Each issuer signs with its OWN DID, because an issuer in VC terms IS a DID: a
# credential's `issuer` is what a verifier checks, so three authorities sharing
# one key would be indistinguishable to any wallet. oid4vc-service signs with the
# `author` of the bound credential schema (oid4vci.service.ts), so minting a DID
# per schema is all that is needed for this to work.
say "4. Issuer DIDs (did:web, so wallets can resolve them)"

ENV_LOCAL="$ROOT/.env.local"

# Mints a did:web, or reuses the one recorded in .env.local when it still
# resolves. Echoes the DID.
mint_did() {
  local var="$1" label="$2" existing did_json did
  existing="$(grep -E "^${var}=" "$ENV_LOCAL" 2>/dev/null | cut -d= -f2- || true)"
  if [ -n "$existing" ] && curl -fsS -o /dev/null "$BASE/${existing##*:}/did.json" 2>/dev/null; then
    ok "reusing $existing ($label)" >&2
    printf '%s' "$existing"
    return 0
  fi
  # `services` must be a non-empty array of objects with a string
  # serviceEndpoint — identity-service rejects the document otherwise, and the
  # error ("serviceEndpoint must be a string") does not say which field.
  did_json="$(curl -fsS -X POST "$BASE/did/generate" \
    -H 'content-type: application/json' \
    -d "{\"content\":[{\"alsoKnownAs\":[\"$label\"],\"method\":\"web\",\"services\":[{\"id\":\"IssuerService\",\"type\":\"LinkedDomains\",\"serviceEndpoint\":\"http://localhost\"}]}]}")"
  did="$(printf '%s' "$did_json" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d[0] if isinstance(d,list) else d)["id"])')"
  [ -n "$did" ] || die "could not mint a DID for $label"
  ok "minted $did ($label)" >&2
  printf '%s' "$did"
}

ISSUER_DID="$(mint_did LOCAL_ISSUER_DID   local-agriculture-authority)"
AGE_DID="$(mint_did    LOCAL_AGE_DID      local-civil-registration-authority)"
EDU_DID="$(mint_did    LOCAL_EDUCATION_DID local-education-board)"

for d in "$ISSUER_DID" "$AGE_DID" "$EDU_DID"; do
  curl -fsS -o /dev/null "$BASE/${d##*:}/did.json" \
    || info "WARNING: $d not resolvable yet"
done

# ISSUER_DID stays the server-wide default (and the VERIFIER_DID) so behaviour is
# unchanged for anything issued before per-issuer DIDs existed.
{
  printf 'LOCAL_ISSUER_DID=%s\n' "$ISSUER_DID"
  printf 'LOCAL_AGE_DID=%s\n' "$AGE_DID"
  printf 'LOCAL_EDUCATION_DID=%s\n' "$EDU_DID"
} > "$ENV_LOCAL"
info "wrote .env.local — restarting oid4vc-service to pick it up"
"${COMPOSE[@]}" --env-file "$ENV_LOCAL" up -d --no-deps oid4vc-service >/dev/null 2>&1 || true
wait_for "oid4vc-service" "$BASE/health" 40

# --- 5. credential schemas, one per issuer -----------------------------------
say "5. Credential schemas"

# The schema body generator lives in a temp file rather than a heredoc inside
# $(...): bash 3.2 — still the default on macOS — cannot parse that combination
# inside a function body, and fails with an unhelpful "unexpected EOF".
SCHEMA_PY="$(mktemp -t schemabody)"
trap 'rm -f "$SCHEMA_PY"' EXIT
cat > "$SCHEMA_PY" <<'PY'
import json, sys
name, sid, author, slug, props, required, desc = sys.argv[1:8]
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
            # MUST allow additional properties. Issuance always adds
            # credentialSubject.id (the holder DID, set in issueForSession in
            # oid4vci.service.ts), which is not one of the schema's own claims.
            # With this false, credentials-service rejects every issuance with a
            # must-NOT-have-additional-properties error, which reaches the wallet
            # only as an opaque 500 Error issuing credential.
            "additionalProperties": True,
        },
    },
    "tags": [slug],
    # PUBLISHED is required: getOid4vciConfigs only looks at published schemas,
    # so a DRAFT one exists but is invisible to issuer metadata.
    "status": "PUBLISHED",
    # Supplied at creation — there is no separate enable call. The flag is
    # `oid4vciEnabled` and the formats key is `oid4vciFormats`; a plausible
    # `enabled`/`formats` is silently ignored and the schema never appears as an
    # issuable credential configuration (schema.service.ts:82).
    "oid4vciConfig": {
        "oid4vciEnabled": True,
        "oid4vciFormats": ["vc+sd-jwt"],
        "vct": "http://localhost/vct/" + slug,
        # `locale` is REQUIRED on a display entry. Without it a wallet fetching
        # the SD-JWT VC Type Metadata fails to parse it and shows only
        # "something went wrong" — with no clue that the cause is here.
        "display": [{"name": name, "locale": "en-US"}],
    },
}))
PY

# Finds a published schema's id by name; empty when absent.
FIND_PY="$(mktemp -t findschema)"
trap 'rm -f "$SCHEMA_PY" "$FIND_PY"' EXIT
cat > "$FIND_PY" <<'PY'
import json, sys
want = sys.argv[1]
for c in json.load(sys.stdin):
    if c.get("name") == want:
        print(c.get("schemaId", ""))
        break
PY

# Creates a PUBLISHED, OID4VCI-enabled vc+sd-jwt schema. Idempotent on name.
# Echoes the schema id, which is also the credential_configuration_id for a
# single-format schema (oid4vci.service.ts:81).
create_schema() {
  local name="$1" sid="$2" author="$3" slug="$4" props="$5" required="$6" desc="$7"
  local body resp existing
  existing="$(curl -fsS "$BASE/credential-schema/oid4vci-configs" | python3 "$FIND_PY" "$name")"
  if [ -n "$existing" ]; then
    ok "$name already present" >&2
    printf '%s' "$existing"
    return 0
  fi

  body="$(python3 "$SCHEMA_PY" "$name" "$sid" "$author" "$slug" "$props" "$required" "$desc")"
  # POST to /credential-schema, not /credential-schema/credential-schema: the
  # service's controller is mounted at that prefix and serves POST at its root.
  resp="$(curl -fsS -X POST "$BASE/credential-schema" -H 'content-type: application/json' -d "$body" 2>&1)"     || die "creating $name failed: $resp"
  printf '%s' "$resp" | python3 -c 'import json,sys; print(json.load(sys.stdin)["schema"]["id"])'     2>/dev/null || die "no schema id returned for $name"
}

# Property names deliberately MATCH the registry field names wherever possible:
# oid4vc-service resolves a claim by name against the configured entities, so a
# well-named schema needs no alias at all. `birthdate` and `primaryCrop` are the
# two exceptions, and they are declared in REGISTRY_CLAIM_ALIASES in the compose
# file rather than in any service's code.
FARMER_CONFIG_ID="$(create_schema \
  'Farmer Land Holding Credential' 'FarmerLandHoldingCredential' "$ISSUER_DID" \
  'farmer-land-holding-credential' \
  '{"farmerId":{"type":"string","description":"Registry-wide unique holder identifier"},
    "name":{"type":"string","description":"Full name"},
    "gender":{"type":"string","description":"Gender as recorded in the registry"},
    "landRecordRef":{"type":"string","description":"Survey or khasra reference for the parcel"},
    "farmLocation":{"type":"string","description":"Village or revenue circle of the parcel"},
    "landAreaAcres":{"type":"number","description":"Parcel area in acres"},
    "ownershipType":{"type":"string","description":"Owned, Leased, Sharecropped or Inherited"},
    "primaryCrop":{"type":"string","description":"Most recent crop sown"}}' \
  '["farmerId","name","landAreaAcres"]' \
  'Land holding of a farmer, issued from the Sunbird RC registry.')"
ok "farmer config: $FARMER_CONFIG_ID"

# Minimal by design: an age credential that carried a land parcel would defeat
# the point of selective disclosure.
AGE_CONFIG_ID="$(create_schema \
  'Mobile Age Credential' 'MobileAgeCredential' "$AGE_DID" \
  'mobile-age-credential' \
  '{"name":{"type":"string","description":"Full name"},
    "birthdate":{"type":"string","description":"Date of birth, ISO 8601"},
    "age_over_18":{"type":"boolean","description":"Derived from the date of birth at issuance"}}' \
  '["name","birthdate"]' \
  'Proof of age, issued from the civil register.')"
ok "age config: $AGE_CONFIG_ID"

EDU_CONFIG_ID="$(create_schema \
  'Education Certificate Credential' 'EducationCertificateCredential' "$EDU_DID" \
  'education-certificate-credential' \
  '{"name":{"type":"string","description":"Full name of the student"},
    "degree":{"type":"string","description":"Award as it appears on the certificate"},
    "institution":{"type":"string","description":"Awarding college, university or board"},
    "yearOfPassing":{"type":"number","description":"Year the award was conferred"},
    "grade":{"type":"string","description":"Grade, class or percentage"}}' \
  '["name","degree","institution"]' \
  'Qualification awarded to a student, issued from the education register.')"
ok "education config: $EDU_CONFIG_ID"

CONFIGS="$(curl -fsS "$BASE/credential-schema/oid4vci-configs")"
COUNT="$(printf '%s' "$CONFIGS" | python3 -c '
import json,sys
print(len([c for c in json.load(sys.stdin) if "vc+sd-jwt" in (c.get("formats") or [])]))')"
[ "$COUNT" -ge 3 ] \
  && ok "$COUNT SD-JWT credential configurations published" \
  || die "expected at least 3 issuable configurations, found $COUNT — check oid4vciEnabled and status"

# --- 6. registry records -----------------------------------------------------
say "6. Registry records"
reg_post() {
  curl -fsS -X POST "$BASE/api/v1/$1" -H 'content-type: application/json' -d "$2" >/dev/null 2>&1
}
reg_search() {
  curl -fsS -X POST "$BASE/api/v1/$1/search" -H 'content-type: application/json' \
    -d '{"offset":0,"limit":1000,"filters":{}}' 2>/dev/null
}
reg_count() {
  reg_search "$1" | python3 -c 'import json,sys
try:
    d=json.load(sys.stdin); print(len(d if isinstance(d,list) else d.get("data",[])))
except Exception: print(0)'
}

# Issuers first: holders point at them, and the portal lists nothing without them.
if [ "$(reg_count Issuer)" -gt 0 ]; then
  ok "$(reg_count Issuer) issuer(s) already present"
else
  # printf, not python: bash 3.2 mangles a multi-line `python3 -c "..."` nested
  # inside "$( )" — it silently drops closing braces, and the resulting error
  # points at a python line that looks fine. A single-quoted format string keeps
  # every double quote and brace intact.
  # Logos are served by the portal itself (issuer-portal/public/logos), so the
  # demo never shows a broken image and needs no external host. `icon` remains as
  # the fallback for an issuer added later with no logo.
  ISSUER_FMT='{"issuerId":"%s","name":"%s","description":"%s","category":"%s","did":"%s","credentialConfigId":"%s","credentialName":"%s","holderLabel":"%s","holderIdPrefix":"%s","recordEntities":"%s","icon":"%s","accent":"%s","logoUrl":"%s","url":"%s","status":"Active"}'

  reg_post Issuer "$(printf "$ISSUER_FMT" \
    'ISS-FARMER' 'Department of Agriculture' \
    'Issues land-holding credentials to registered farmers, from the land and crop records held in the registry.' \
    'Agriculture' "$ISSUER_DID" "$FARMER_CONFIG_ID" 'Farmer Land Holding Credential' \
    'Farmer' 'FRM' 'LandParcel,Crop,SeedDistribution' '🌾' '#4a7c59' \
    '/issuer-portal/logos/agriculture.svg' 'https://agricoop.gov.in')" \
    && ok "Issuer ISS-FARMER (Department of Agriculture)" || info "ISS-FARMER failed"

  reg_post Issuer "$(printf "$ISSUER_FMT" \
    'ISS-AGE' 'Civil Registration Authority' \
    'Issues a minimal age credential — the holder proves they are over 18 without revealing anything else.' \
    'Identity' "$AGE_DID" "$AGE_CONFIG_ID" 'Mobile Age Credential' \
    'Citizen' 'AGE' '' '🪪' '#3d6b8c' \
    '/issuer-portal/logos/civil-registration.svg' 'https://crsorgi.gov.in')" \
    && ok "Issuer ISS-AGE (Civil Registration Authority)" || info "ISS-AGE failed"

  reg_post Issuer "$(printf "$ISSUER_FMT" \
    'ISS-EDUCATION' 'State Board of Education' \
    'Issues qualification credentials to students from the awards on their record.' \
    'Education' "$EDU_DID" "$EDU_CONFIG_ID" 'Education Certificate Credential' \
    'Student' 'EDU' 'Qualification' '🎓' '#8c5a3d' \
    '/issuer-portal/logos/education.svg' 'https://education.gov.in')" \
    && ok "Issuer ISS-EDUCATION (State Board of Education)" || info "ISS-EDUCATION failed"
fi

# Per-record idempotence, not "any holder exists -> skip everything": a registry
# seeded before the other two issuers existed would otherwise never get their
# holders, and the script would report success having done nothing.
ALL_IDS="$(reg_search Farmer | python3 -c '
import json,sys
try: d = json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d, list) else d.get("data", [])
print(" ".join(r.get("farmerId","") for r in rows))')"

# Posts a holder unless that id is already in the registry.
seed_holder() {
  local fid="$1" body="$2" label="$3"
  case " $ALL_IDS " in
    *" $fid "*) info "$fid already present"; return 0 ;;
  esac
  reg_post Farmer "$body" && ok "$label" || info "$fid failed"
}

seed_holder FRM-000123 '{"issuerId":"ISS-FARMER","farmerId":"FRM-000123","name":"Ravi Kumar","gender":"Male","dateOfBirth":"1979-04-12","mobile":"+91 98450 11223","district":"Mandya","state":"Karnataka","keycloakUsername":"farmer.ravi"}' \
  'Farmer FRM-000123 (Ravi Kumar)'
seed_holder FRM-000124 '{"issuerId":"ISS-FARMER","farmerId":"FRM-000124","name":"Lakshmi Devi","gender":"Female","dateOfBirth":"1986-11-03","district":"Hassan","state":"Karnataka","keycloakUsername":"farmer.lakshmi"}' \
  'Farmer FRM-000124 (Lakshmi Devi)'
# No parcel on purpose: exercises the blocked-issuance path in the portal.
seed_holder FRM-000125 '{"issuerId":"ISS-FARMER","farmerId":"FRM-000125","name":"Anand Patil","gender":"Male","district":"Belagavi","state":"Karnataka"}' \
  'Farmer FRM-000125 (no land — negative case)'

seed_holder AGE-000001 '{"issuerId":"ISS-AGE","farmerId":"AGE-000001","name":"Meera Nair","gender":"Female","dateOfBirth":"2001-03-19","district":"Ernakulam","state":"Kerala","keycloakUsername":"citizen.meera"}' \
  'Citizen AGE-000001 (Meera Nair, adult)'
# A minor on purpose: age_over_18 resolves to false, which must still be a claim
# rather than a missing value.
seed_holder AGE-000002 '{"issuerId":"ISS-AGE","farmerId":"AGE-000002","name":"Arjun Das","gender":"Male","dateOfBirth":"2012-08-30","district":"Kozhikode","state":"Kerala"}' \
  'Citizen AGE-000002 (Arjun Das, minor — age_over_18 false)'

seed_holder EDU-000001 '{"issuerId":"ISS-EDUCATION","farmerId":"EDU-000001","name":"Priya Sharma","gender":"Female","dateOfBirth":"1998-06-05","district":"Bengaluru Urban","state":"Karnataka","keycloakUsername":"student.priya"}' \
  'Student EDU-000001 (Priya Sharma)'
# No qualification on purpose: the Education issuer's blocked-issuance case.
seed_holder EDU-000002 '{"issuerId":"ISS-EDUCATION","farmerId":"EDU-000002","name":"Rahul Verma","gender":"Male","district":"Mysuru","state":"Karnataka"}' \
  'Student EDU-000002 (no qualification — negative case)'

# Child records, guarded on their own entity counts: they carry no natural key
# this script can cheaply check, so a count of zero is the signal that none have
# been seeded yet.
if [ "$(reg_count LandParcel)" -gt 0 ]; then
  info "$(reg_count LandParcel) land parcel(s) already present"
else
  reg_post LandParcel '{"farmerId":"FRM-000123","landRecordRef":"LR-KA-77-2201","farmLocation":"Rampur, Mandya","landAreaAcres":4.5,"ownershipType":"Owned","surveyedOn":"2024-06-01"}' \
    && ok "LandParcel LR-KA-77-2201 (4.5 acres, Owned)" || info "parcel 1 failed"
  reg_post LandParcel '{"farmerId":"FRM-000123","landRecordRef":"LR-KA-77-2202","farmLocation":"Rampur, Mandya","landAreaAcres":1.75,"ownershipType":"Leased"}' \
    && ok "LandParcel LR-KA-77-2202 (1.75 acres, Leased) — second parcel, exercises the picker" || info "parcel 2 failed"
  reg_post LandParcel '{"farmerId":"FRM-000124","landRecordRef":"LR-KA-88-3302","farmLocation":"Shivpur, Hassan","landAreaAcres":2,"ownershipType":"Leased"}' \
    && ok "LandParcel LR-KA-88-3302" || info "parcel 3 failed"
fi

if [ "$(reg_count Crop)" -gt 0 ]; then
  info "$(reg_count Crop) crop cycle(s) already present"
else
  reg_post Crop '{"farmerId":"FRM-000123","landRecordRef":"LR-KA-77-2201","cropName":"Wheat","season":"Rabi","year":2026,"areaSownAcres":3.5}' \
    && ok "Crop Wheat 2026" || info "crop 1 failed"
  reg_post Crop '{"farmerId":"FRM-000124","cropName":"Ragi","season":"Kharif","year":2026,"areaSownAcres":2}' \
    && ok "Crop Ragi 2026" || info "crop 2 failed"
fi

if [ "$(reg_count SeedDistribution)" -gt 0 ]; then
  info "$(reg_count SeedDistribution) seed record(s) already present"
else
  reg_post SeedDistribution '{"farmerId":"FRM-000123","seedType":"Wheat","variety":"HD-2967","quantityKg":40,"issuedOn":"2026-10-12","subsidyScheme":"NFSM"}' \
    && ok "SeedDistribution Wheat 40kg" || info "seed failed"
fi

if [ "$(reg_count Qualification)" -gt 0 ]; then
  info "$(reg_count Qualification) qualification(s) already present"
else
  reg_post Qualification '{"farmerId":"EDU-000001","degree":"B.Sc. Agriculture","institution":"University of Agricultural Sciences, Bengaluru","yearOfPassing":2020,"grade":"First Class","enrolmentNumber":"UAS-2016-4471"}' \
    && ok "Qualification B.Sc. Agriculture 2020" || info "qualification 1 failed"
  # An earlier award, so "most recent wins" is observable in the claim table.
  reg_post Qualification '{"farmerId":"EDU-000001","degree":"Higher Secondary","institution":"Karnataka State Board","yearOfPassing":2016,"grade":"88%"}' \
    && ok "Qualification Higher Secondary 2016 (older — must NOT be the one issued)" || info "qualification 2 failed"
fi

# Branding backfill. The seeding block above only runs on an empty registry, so a
# deployment seeded before logos existed would keep showing the emoji fallback
# forever. Setting them here means re-running this script upgrades the demo.
say "6b. Issuer branding"
brand() {
  local iid="$1" logo="$2" site="$3" row osid body
  row="$(reg_search Issuer | python3 -c '
import json, sys
want = sys.argv[1]
try: d = json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d, list) else d.get("data", [])
for r in rows:
    if r.get("issuerId") == want:
        print(json.dumps(r)); break' "$iid")"
  [ -n "$row" ] || { info "$iid not present"; return 0; }
  if printf '%s' "$row" | grep -q '"logoUrl"'; then
    info "$iid already branded"
    return 0
  fi
  osid="$(printf '%s' "$row" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("osid",""))')"
  body="$(printf '%s' "$row" | python3 -c '
import json, sys
r = json.load(sys.stdin)
r["logoUrl"] = sys.argv[1]
r["url"] = sys.argv[2]
r.pop("osid", None)
print(json.dumps(r))' "$logo" "$site")"
  curl -fsS -X PUT "$BASE/api/v1/Issuer/$osid" -H 'content-type: application/json' -d "$body" >/dev/null 2>&1 \
    && ok "$iid branded" || info "could not brand $iid"
}
brand ISS-FARMER    '/issuer-portal/logos/agriculture.svg'        'https://agricoop.gov.in'
brand ISS-AGE       '/issuer-portal/logos/civil-registration.svg' 'https://crsorgi.gov.in'
brand ISS-EDUCATION '/issuer-portal/logos/education.svg'          'https://education.gov.in'

# Backfill: holders created before issuers existed have no issuerId, so no
# issuer's list would show them and the portal would appear to have lost them.
say "6c. Backfilling holders with no issuer"
UNASSIGNED="$(reg_search Farmer | python3 -c '
import json,sys
try: d=json.load(sys.stdin)
except Exception: raise SystemExit
rows = d if isinstance(d,list) else d.get("data",[])
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
    # Guess from the id prefix, defaulting to the farmer issuer — these are
    # records that predate the issuer concept, which on this deployment means
    # they were all farmers.
    case "$FID" in
      AGE-*) TARGET=ISS-AGE ;;
      EDU-*) TARGET=ISS-EDUCATION ;;
      *)     TARGET=ISS-FARMER ;;
    esac
    BODY="$(printf '%s' "$row" | python3 -c '
import json,sys
r = json.load(sys.stdin)
r["issuerId"] = sys.argv[1]
r.pop("osid", None)
print(json.dumps(r))' "$TARGET")"
    if curl -fsS -X PUT "$BASE/api/v1/Farmer/$OSID" -H 'content-type: application/json' \
        -d "$BODY" >/dev/null 2>&1; then
      ok "$FID -> $TARGET"
    else
      info "could not backfill $FID"
    fi
  done
fi

# --- 7. summary --------------------------------------------------------------
say "Ready"
cat <<EOF
  Issuer portal   http://localhost/issuer-portal/
  Verifier        http://localhost/verifier-app/
  Keycloak admin  http://localhost/auth/admin   (admin / admin123)

  Staff login     issuer.staff    / Passw0rd!

  Three issuers, each with its own did:web and its own credential type:

    Department of Agriculture      Farmer Land Holding Credential
      farmer.ravi     / Passw0rd!  -> FRM-000123 (2 parcels, so the picker appears)
      farmer.lakshmi  / Passw0rd!  -> FRM-000124
                                     FRM-000125 (no land — issuance BLOCKED)

    Civil Registration Authority  Mobile Age Credential
      citizen.meera   / Passw0rd!  -> AGE-000001 (adult, age_over_18 true)
                                     AGE-000002 (minor, age_over_18 false)

    State Board of Education      Education Certificate Credential
      student.priya   / Passw0rd!  -> EDU-000001 (two awards, newest is issued)
                                     EDU-000002 (no award — issuance BLOCKED)

    farmer.norecord / Passw0rd!    -> no record at all (negative case)

  Try, in order:
    1. Sign in as issuer.staff. You land on Issuers — three cards, each showing
       its credential type and holder count.
    2. Open Department of Agriculture -> FRM-000123 -> Issue credential. Two
       parcels, so the picker appears; the claim table shows which record each
       value came from.
    3. Open State Board of Education -> EDU-000002 -> Issue credential. Issuance
       is BLOCKED on degree and institution, because that student has no award.
    4. Add a new issuer with "+ New issuer", bind a credential type to it, then
       add a holder — the ID is suggested from the prefix you chose.
    5. Back on any holder with a wallet login, generate an offer with "signs in".
       Scan the QR with a wallet: it asks for that holder's username and password
       and issues only their own credential.
    6. Verify the result at http://localhost/verifier-app/.
EOF
