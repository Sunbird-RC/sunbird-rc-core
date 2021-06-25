# OpenSaber-RC Infra setup

### Docker compose

##### Prerequisite
java 8

##### Start Postgres and Elastic Search

```sh
cd java/registry
docker-compose up -d db es
```
```sh
sh configure-dependencies.sh
cd java/
./mvnw clean install -DskipTests
java -jar registry/target/registry.jar
```

### dependencies
* Elastic search https://hub.kubeapps.com/charts/bitnami/elasticsearch
* Postgres : https://hub.kubeapps.com/charts/cetic/postgresql
