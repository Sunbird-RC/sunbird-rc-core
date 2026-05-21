#!/usr/bin/env bash
# enable-v2.sh — Enables Signature V2, DID, Claims and all required services.
# Services brought up: vault, identity, credential-schema, credential, claim-ms
# Registry is restarted with signature/did/claims enabled.
#
# Usage: bash enable-v2.sh

set -uo pipefail   # note: no -e so we can handle vault's non-zero exit codes

COMPOSE="docker compose"
ENV_FILE=".env"
KEYS_FILE="keys.txt"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

cd "$(dirname "$0")"

# ── helpers ───────────────────────────────────────────────────────────────────
patch_env() {
    local key="$1" val="$2"
    if grep -qE "^${key}=" "$ENV_FILE" 2>/dev/null; then
        warn "$key already in $ENV_FILE — skipping"
    else
        echo "${key}=${val}" >> "$ENV_FILE"
        info "Added ${key}"
    fi
}

container_running() {
    $COMPOSE ps --status running "$1" 2>/dev/null | grep -q "$1"
}

wait_container() {
    local svc="$1" max="${2:-60}"
    info "Waiting for container '$svc' to start..."
    for i in $(seq 1 "$max"); do
        if container_running "$svc"; then
            info "Container '$svc' is running."
            return 0
        fi
        sleep 3
    done
    error "Container '$svc' did not start within $((max * 3))s. Check: docker compose logs $svc"
}

wait_http() {
    local svc="$1" url="$2" max="${3:-40}"
    info "Waiting for $svc health at $url ..."
    for i in $(seq 1 "$max"); do
        if $COMPOSE exec -T "$svc" curl -sf "$url" >/dev/null 2>&1; then
            info "$svc is healthy."
            return 0
        fi
        sleep 3
    done
    error "$svc did not become healthy in $((max * 3))s. Check: docker compose logs $svc"
}

# ── 1. Patch .env ─────────────────────────────────────────────────────────────
info "Patching $ENV_FILE ..."

grep -qE "^VAULT_TOKEN=" "$ENV_FILE" 2>/dev/null || echo "VAULT_TOKEN=" >> "$ENV_FILE"

patch_env "SIGNATURE_ENABLED"           "true"
patch_env "SIGNATURE_PROVIDER"          "dev.sunbirdrc.registry.service.impl.SignatureV2ServiceImpl"
patch_env "DID_ENABLED"                 "true"
patch_env "CLAIMS_ENABLED"              "true"
patch_env "VAULT_ADDR"                  "http://vault:8200"
patch_env "VAULT_API_ADDR"              "http://vault:8200"
patch_env "VAULT_ADDRESS"               "http://vault:8200"
patch_env "VAULT_BASE_URL"              "http://vault:8200/v1/"
patch_env "VAULT_ROOT_PATH"             "kv"
patch_env "VAULT_TIMEOUT"               "5000"
patch_env "VAULT_PROXY"                 "false"
patch_env "IDENTITY_BASE_URL"           "http://identity:3332"
patch_env "SCHEMA_BASE_URL"             "http://credential-schema:3333"
patch_env "CREDENTIAL_SERVICE_BASE_URL" "http://credential:3000"
patch_env "SIGNING_ALGORITHM"           "Ed25519Signature2020"
patch_env "WEB_DID_BASE_URL"            "http://localhost:3332"
patch_env "ENABLE_AUTH"                 "false"
patch_env "JWKS_URI"                    ""
patch_env "QR_TYPE"                     "W3C_VC"

# ── 2. Start Vault ─────────────────────────────────────────────────────────────
info "Pulling and starting vault (this may take a moment on first run)..."
$COMPOSE pull vault 2>&1 | grep -v "^$" || true
$COMPOSE up -d vault

wait_container vault 30

# Wait until vault API responds (vault status exits 0 unsealed, 2 sealed — both mean it's up)
info "Waiting for vault API to respond..."
for i in $(seq 1 30); do
    VAULT_OUT=$($COMPOSE exec -T vault vault status 2>&1 || true)
    if echo "$VAULT_OUT" | grep -qE "Initialized|Sealed"; then
        info "Vault API is up."
        break
    fi
    sleep 3
    [[ $i -eq 30 ]] && error "Vault API did not respond within 90s"
done

# ── 3. Initialize Vault (first run only) ───────────────────────────────────────
VAULT_STATUS=$($COMPOSE exec -T vault vault status 2>&1 || true)

if echo "$VAULT_STATUS" | grep -q "Initialized.*true"; then
    info "Vault already initialized."
else
    info "Initializing vault for the first time..."
    $COMPOSE exec -T vault vault operator init > ansi-keys.txt 2>&1
    sed 's/\x1B\[[0-9;]*[JKmsu]//g' < ansi-keys.txt > "$KEYS_FILE"
    rm -f ansi-keys.txt
    info "Vault initialized. Keys saved to $KEYS_FILE — keep this file safe!"
fi

# ── 4. Unseal Vault ────────────────────────────────────────────────────────────
VAULT_STATUS=$($COMPOSE exec -T vault vault status 2>&1 || true)

if echo "$VAULT_STATUS" | grep -q "Sealed.*false"; then
    info "Vault already unsealed."
else
    info "Unsealing vault (applying keys 1, 2, 3 from $KEYS_FILE)..."
    [[ ! -f "$KEYS_FILE" ]] && error "$KEYS_FILE not found. Cannot unseal vault."

    for i in 1 2 3; do
        KEY=$(sed -n "s/Unseal Key ${i}: \(.*\)/\1/p" "$KEYS_FILE" | tr -dc '[:print:]')
        [[ -z "$KEY" ]] && error "Could not read Unseal Key $i from $KEYS_FILE"
        $COMPOSE exec -T vault vault operator unseal "$KEY" >/dev/null
        info "Applied unseal key $i"
    done
fi

# ── 5. Write root token to .env ────────────────────────────────────────────────
if [[ -f "$KEYS_FILE" ]]; then
    ROOT_TOKEN=$(sed -n 's/Initial Root Token: \(.*\)/\1/p' "$KEYS_FILE" | tr -dc '[:print:]' || true)
    if [[ -n "$ROOT_TOKEN" ]]; then
        CURRENT=$(grep "^VAULT_TOKEN=" "$ENV_FILE" | cut -d= -f2-)
        if [[ -z "$CURRENT" ]]; then
            sed -i.bak "s|^VAULT_TOKEN=.*|VAULT_TOKEN=${ROOT_TOKEN}|" "$ENV_FILE"
            info "VAULT_TOKEN written to $ENV_FILE"
        else
            warn "VAULT_TOKEN already has a value — not overwriting"
        fi
    fi
fi

# ── 6. Enable KV secrets engine (first run only) ───────────────────────────────
CURRENT_TOKEN=$(grep "^VAULT_TOKEN=" "$ENV_FILE" | cut -d= -f2- | tr -dc '[:print:]')
[[ -z "$CURRENT_TOKEN" ]] && error "VAULT_TOKEN is empty in $ENV_FILE. Cannot continue."

SECRETS=$($COMPOSE exec -T -e VAULT_TOKEN="$CURRENT_TOKEN" vault vault secrets list 2>&1 || true)
if echo "$SECRETS" | grep -q "^kv/"; then
    info "KV secrets engine already enabled at kv/."
else
    info "Enabling kv-v2 secrets engine at path 'kv'..."
    $COMPOSE exec -T -e VAULT_TOKEN="$CURRENT_TOKEN" vault vault secrets enable -path=kv kv-v2
fi

# ── 7. Create dedicated databases for V2 Node.js services ─────────────────────
# Each service needs its own clean DB so Prisma migrations don't conflict
# with the Java registry schema already in the 'registry' database.
info "Creating dedicated databases for V2 services (if not exist)..."
for DBNAME in identity credential_schema credential; do
    EXISTS=$($COMPOSE exec -T db psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DBNAME}'" 2>/dev/null || true)
    if [[ "$EXISTS" == "1" ]]; then
        info "Database '$DBNAME' already exists."
    else
        $COMPOSE exec -T db psql -U postgres -c "CREATE DATABASE ${DBNAME};" >/dev/null
        info "Created database '$DBNAME'."
    fi
done

# ── 8. Identity ────────────────────────────────────────────────────────────────
info "Starting identity..."
$COMPOSE up -d identity
wait_container identity 30
wait_http identity "http://localhost:3332/health" 30

# ── 9. Credential-schema ───────────────────────────────────────────────────────
info "Starting credential-schema..."
$COMPOSE up -d credential-schema
wait_container credential-schema 30
wait_http credential-schema "http://localhost:3333/health" 30

# ── 10. Credential ─────────────────────────────────────────────────────────────
info "Starting credential..."
$COMPOSE up -d credential
wait_container credential 30
wait_http credential "http://localhost:3000/health" 30

# ── 11. Claim-ms ───────────────────────────────────────────────────────────────
info "Starting claim-ms..."
$COMPOSE up -d claim-ms
wait_container claim-ms 20

# ── 12. Restart registry ───────────────────────────────────────────────────────
info "Restarting registry to pick up new env vars..."
$COMPOSE up -d --force-recreate registry

info "Waiting for registry health at localhost:8091/health..."
for i in $(seq 1 40); do
    if curl -sf http://localhost:8091/health >/dev/null 2>&1; then
        info "Registry is healthy."
        break
    fi
    sleep 5
    [[ $i -eq 40 ]] && warn "Registry health check timed out. Check: docker compose logs registry"
done

# ── Done ───────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}══════════════════════════════════════════════════════${NC}"
echo -e "${GREEN} V2 setup complete!${NC}"
echo -e "${GREEN}══════════════════════════════════════════════════════${NC}"
echo ""
echo "  Vault:             http://localhost:8200"
echo "  Identity (DID):    http://localhost:3332"
echo "  Credential Schema: http://localhost:3333"
echo "  Credential:        http://localhost:3000"
echo "  Claim MS:          http://localhost:8082"
echo "  Registry:          http://localhost:8091"
echo ""
echo "  Test sign endpoint:"
echo "  curl http://localhost:8091/api/v1/Teacher/sign -H 'Accept: application/json'"
echo ""
echo -e "${YELLOW}  IMPORTANT: $KEYS_FILE contains your vault unseal keys.${NC}"
echo -e "${YELLOW}  Run this script again after any docker compose down to re-unseal.${NC}"
