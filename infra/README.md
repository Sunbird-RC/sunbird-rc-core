# Production deployment using Helm charts
The below scripts will help the adopters to deploy SunbirdRC services in kubernetes environment.

## Prerequisites
- Kubernetes Cluster with minimum 3 nodes
- [Helm](https://helm.sh/docs/intro/install/)
- kubectl
- Ingress
- Postgres DB (create database for `keycloak` and `registry`)
- ElasticSearch (Optional)
- Kafka (Optional)
- Redis (Optional)
- Minio (Optional)
- Domain URL (domain url mapped to kubernetes cluster)

The above optional services are not mandatory for SunbirdRC services. It can be installed based on the requirement on the project. For more details https://docs.sunbirdrc.dev/learn/readme-1/high-level-architecture

## Deployment steps

### Clone the repo
```bash
git clone https://github.com/Sunbird-RC/sunbird-rc-core.git
cd infra
```

### Pre check
Make sure from the current directory you're able to run the below commands
```bash
kubectl cluster-info
kubectl get nodes
kubectl get ns
helm version
```

### Create namespace
```bash
kubectl create ns demo-registry
```
`Feel free to use a different name for the namespace. Use the same name in the reset of the commands.`

### Create secrets
Convert all the passwords/secrets into base64 format and update these values in `values.yaml` file
**Secrets**
- DB_PASSWORD: Postgres database password
- KEYCLOAK_ADMIN_PASSWORD: Keycloak admin password used to login to admin console
- KEYCLOAK_DEFAULT_USER_PASSWORD: Default password to be set for new users created by registry
- MINIO_SECRET_KEY: Minio secret key 
- ELASTIC_SEARCH_PASSWORD: Elastic search connection password
- KEYCLOAK_ADMIN_CLIENT_SECRET: Client secret of keycloak admin client for registry

`DB_PASSWORD, KEYCLOAK_ADMIN_PASSWORD and KEYCLOAK_DEFAULT_USER_PASSWORD are mandotry secrets to be set. Other secrets can be set to empty `

### Modify configuration values
Configuration values like database address, elastic search address etc should be modified in values.yaml file.


### Schemas
All schema files should be placed in the schemas directory located at `sunbird-rc-core/infra/helm_charts/charts/registry/schemas`.

### Configure signing keys
The signing keys should be placed in the below directories

Both public and private keys for signing

`sunbird-rc-core/infra/helm_charts/charts/certificate-signer/keys`

Only public key for exposing to verifiers

`sunbird-rc-core/infra/helm_charts/charts/public-key-service/keys`
# Please note that by default a sample key is added. It is highly recommended to update this key before going to production.

### Deploy helm charts
```bash
helm upgrade --install --namespace=demo-registry demo-registry helm_charts --create-namespace
```
**Output**
```
Release "demo-registry" does not exist. Installing it now.
NAME: demo-registry
LAST DEPLOYED: Thu May  4 17:02:08 2023
NAMESPACE: demo-registry
STATUS: deployed
REVISION: 1
```

**Check if all the pods are running**
```bash
kubectl get pods -n demo-registry
```

### Import keycloak realm

- Goto keycloak admin console `<host>/auth/`
- Login with username `admin` and use the same password configured in secrets
- Click on `Master` and select `Add realm`
- Select `https://github.com/Sunbird-RC/sunbird-rc-core/blob/main/imports/realm-export.json` file 
- And click on `Create`


### Configure keycloak secret

**Get keycloak secret from keycloak admin console**
- Goto keycloak admin console `<host>/auth/`
- Login with username `admin` and use the same password configured in secrets
- Goto `clients` page and click on `admin-api`
- Goto `Credentials` tab and click on `Regenerate Secret`
- Copy the secret

**Configure secret in registry**
- Get all secrets created
```bash
kubectl get secret -n demo-registry
```
- Encode the secret in base64 format
```bash
echo -n "secret copied from keycloak" | base64
```
- Open the secret in edit mode
```bash
kubectl edit secret rc-secret -n demo-registry
```
Replace empty string for `KEYCLOAK_ADMIN_CLIENT_SECRET` with the base64 encoded secret
- Restart registry
```bash
kubectl rollout restart deploy/demo-registry -n demo-registry
```
- Check the pods status
```bash
kubectl get pods -n demo-registry
```

### Check registry apis
Open the below url in browser and check if you're able to get the swagger json
`<host>/registry/api/docs/swagger.json`

