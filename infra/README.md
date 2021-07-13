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


#Infra setup

### Install docker
sudo apt-get install docker.io

### Give sudo access to docker
sudo groupadd docker

echo $USER

sudo usermod -aG docker $USER

id

## Minikube setup
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64

sudo install minikube-linux-amd64 /usr/local/bin/minikube

minikube start


### Install kubectl
sudo snap install kubectl --classic

### Helm setup
curl https://baltocdn.com/helm/signing.asc | sudo apt-key add -

sudo apt-get install apt-transport-https --yes

echo "deb https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list

sudo apt-get update

sudo apt-get install helm

### Add postgres kubeapps
helm repo add bitnami https://charts.bitnami.com/bitnami

helm install ```APP_NAME``` bitnami/postgresql

### Install postgres client
sudo apt install postgresql-client-common

sudo apt-get install postgresql-client-12

Use helm status ```APP_NAME``` to get the details about the app.

### Add elasticsearch kubeapps
helm repo add bitnami https://charts.bitnami.com/bitnami

helm install bitnami/elasticsearch --version 15.9.1 --generate-name

NOTE: ES setup might take sometime to use

Use helm status ```APP_NAME``` to get the details about the app.

### Kubectl commands
To create namespace: kubectl create ns <name>

To apply deployment file: kubectl -n <namespace> apply -f <deployment yaml>

To get all the pods: kubectl -n <namespace> get pods

To get all the services: kubectl -n <namespace> get svc

