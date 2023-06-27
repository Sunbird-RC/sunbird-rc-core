#!/bin/bash

# This script does the following things
# * Start a vault instance with vault/vault.json configuration
# * Unseal the vault
# * Create a v2 kv engine 
# * Prints the unseal keys and root token
# * This script does not automatically unseal vault on restarts, it only works with fresh installations


docker-compose up -d vault

# Function to check if Vault is ready
check_vault_status() {
  vault_status=$(docker-compose exec vault vault status 2>&1)
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

docker-compose exec vault vault operator init > keys.txt
sed -n -e 's/^Unseal Key [1-1]: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose exec -T vault vault operator unseal "$key"

sed -n -e 's/^Unseal Key [2-2]: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose exec -T vault vault operator unseal "$key"

sed -n -e 's/^Unseal Key [3-3]: \(.*\)/\1/p' keys.txt > parsed-key.txt
key=$(cat parsed-key.txt)
docker-compose exec -T vault vault operator unseal "$key"

root_token=$(sed -n 's/Initial Root Token: \(.*\)/\1/p' keys.txt)

sed -i "s/VAULT_TOKEN:.*/VAULT_TOKEN: $root_token/" docker-compose.yml

docker-compose exec -e VAULT_TOKEN=$root_token -T vault vault secrets enable -path=kv kv-v2

cat keys.txt
rm parsed-key.txt