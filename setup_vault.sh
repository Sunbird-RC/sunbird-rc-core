#!/bin/bash

# This script does the following things
# * Start a vault instance with vault/vault.json configuration
# * Unseal the vault
# * Create a v2 kv engine
# * Prints the unseal keys and root token
# * This script does not automatically unseal vault on restarts, it only works with fresh installations

COMPOSE_FILE="${1:-docker-compose.yml}"
SERVICE_NAME="${2:-vault}"

echo "Setting up $SERVICE_NAME in $COMPOSE_FILE"

docker-compose -f "$COMPOSE_FILE" up -d "$SERVICE_NAME"

# Function to check if Vault is ready
check_vault_status() {
  vault_status=$(docker-compose -f "$COMPOSE_FILE" exec "$SERVICE_NAME" vault status 2>&1)
  if [[ $vault_status == *"connection refused"* ]]; then
    echo "Unable to connect to Vault. Waiting for Vault to start..."
    return 1
  elif [[ $vault_status == *"Sealed             true"* ]]; then
    echo "Vault is sealed. Waiting for unsealing..."
    return 0
  else
    echo "Unsealed and up. Moving to next steps."
    return 0
  fi
}


# Wait for Vault service to become available
until check_vault_status; do
    echo "Waiting for Vault service to start..."
    sleep 1;
done


if [[ $vault_status == *"Initialized     true"* ]]; then
    echo "Vault is initialized already. Unsealing if it is not unsealed"
else
  # keys contains ansi escape sequences, remove them if any
  docker-compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" vault operator init > ansi-keys.txt
  sed 's/\x1B\[[0-9;]*[JKmsu]//g' < ansi-keys.txt  > keys.txt
fi

sed -n 's/Unseal Key [1-1]\+: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" vault operator unseal "$key" < /dev/null

sed -n 's/Unseal Key [2-2]\+: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" vault operator unseal "$key" < /dev/null

sed -n 's/Unseal Key [3-3]\+: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" vault operator unseal "$key" < /dev/null

root_token=$(sed -n 's/Initial Root Token: \(.*\)/\1/p' keys.txt | tr -dc '[:print:]')

if [[ $vault_status == *"Initialized     true"* ]]; then
    echo "Vault is initialized already. Skipping creating a KV engine"
else
  sed -i "s/VAULT_TOKEN=.*/VAULT_TOKEN=$root_token/" ".env"
  docker-compose -f "$COMPOSE_FILE" exec -e VAULT_TOKEN=$root_token -T "$SERVICE_NAME" vault secrets enable -path=kv kv-v2
fi

echo -e "\nNOTE: KEYS ARE STORED IN keys.txt"

if [ -f "ansi-keys.txt" ] ; then
    rm ansi-keys.txt
fi

if [ -f "parsed-key.txt" ] ; then
    rm parsed-key.txt
fi
