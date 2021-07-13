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



### Minikube setup history
22  curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
23  sudo install minikube-linux-amd64 /usr/local/bin/minikube
24  minikube status
25  minikube start
26  sudo apt-get install docker-io
27  sudo apt-get install docker.io
28  minikube start
29  docker ps
30  sudo docker ps
31  sudo minikube start
32  docker system prune
33  sudo docker system prune
34  sudo groupadd docker
35  $USER
36  echo $USER
37  sudo usermod -aG docker $USER
38  exit
39  id
40  docker ps
41  minikube start
42  curl https://baltocdn.com/helm/signing.asc | sudo apt-key add -
43  sudo apt-get install apt-transport-https --yes
44  echo "deb https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
45  helm
46  sudo apt-get update
47  sudo apt-get install helm
48  helm repo add bitnami https://charts.bitnami.com/bitnami
49  helm install my-release bitnami/postgresql
50  helm repo add bitnami https://charts.bitnami.com/bitnami
51  helm install bitnami/elasticsearch --version 15.9.1 --generate-name
52  sudo snap install kubectl
53  sudo apt-get install kubectl
54  sudo snap install kubectl --classic
55  export POSTGRES_PASSWORD=$(kubectl get secret --namespace default my-release-postgresql -o jsonpath="{.data.postgresql-password}" | base64 --decode)
56  echo $(export POSTGRES_PASSWORD=$(kubectl get secret --namespace default my-release-postgresql -o jsonpath="{.data.postgresql-password}" | base64 --decode))
57  echo $POSTGRES_PASSWORD
58  psql
59  sudo apt install postgresql-client-common
60  psql
61  sudo apt-get install postgres-client-12
62  sudo apt-get install postgresql-client-12
63  psql
64  kubectl port-forward --namespace default svc/my-release-postgresql 5432:5432 &
65  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
66  mkdir ndear
67  mkdir infra
68  cd infra/
69  cd ../
70  rm -r infra
71  cd ndear/
72  mkdir infra
73  cd infra/
74  vi registry-deployment.yaml
75  kubectl create ns ndear
76  use ndear
77  kubectl -n ndear apply -f registry-deployment.yaml
78  kubectl -n ndear get pods
79  kubectl -n ndear get pods -w
80  kubectl -n ndear logs -f
81  kubectl -n ndear logs -f registry-65d5994c75-sfkrc
82  kubectl get ns
83  python
84  python3
85  pip3 install pyngrok
86  pip install pyngrok
87  sudo apt install python3-pip
88  pip3 install pyngrok
89  ngrok http 80
90  ngrok http 9200
91  sudo snap install ngrok
92  ngrok http 9200
93  kubectl port-forward -n kubeapps svc/kubeapps 8081:80
94  kubectl port-forward -n default svc/kubeapps 8081:80
95  kubectl get svc
96  kubectl get pods
97  kubectl logs -f elasticsearch-1626069522-master-0
98  helm status bitnami/elasticsearch
99  helm status elasticsearch
100  helm list
101  helm status elasticsearch-1626069522.3
102  helm status elasticsearch-1626069522
103  helm delete elasticsearch-1626069522
104  helm install ndear-es bitnami/elasticsearch --version 15.9.0
105  helm list
106  helm status ndear-es
107  kubectl get pods
108  htop
109  kubectl get pods -w
110  vi registry-deployment.yaml
111  kubectl -n ndear apply -f registry-deployment.yaml
112  kubectl -n ndear get pods -w
113  kubectl -n ndear logs -f registry-b5845cb9-lfcps
114  kubectl -n ndear exec -it registry-b5845cb9-lfcps
115  kubectl -n ndear exec -it registry-b5845cb9-lfcps sh
116  vi registry-deployment.yaml
117  history
118  kubect -n ndear get pods
119  kubectl -n ndear get pods
120  kubectl -n ndear logs -f registry-b5845cb9-lfcps
121  kubcetcl get pods
122  kubcectl get pods
123  kubectl get pods
124  kubectl get svc
125  cat registry-deployment.yaml
126  helm list
127  helm status ndear-es
128  fg
129  kubectl port-forward --namespace default svc/ndear-es-elasticsearch-master 9200:9200 &
130  curl http://127.0.0.1:9200/
131  kubectl get pods
132  kubectl logs -f ndear-es-elasticsearch-master-0
133  curl http://127.0.0.1:9200/
134  kubectl -n ndear rollout restart deployment/registry
135  kubectl -n ndear logs -f deployment/registry
136  fg
137  vi registry-deployment.yaml
138  kubectl port-forward --namespace default svc/ndear-es-elasticsearch-coordinating-only 9200:9200
139  kubectl port-forward --namespace default svc/ndear-es-elasticsearch-coordinating-only 9200:9200 &
140  curl http://127.0.0.1
141  curl http://127.0.0.1:9200
142  sudo apt-get install nginx
143  curl localhost
144  kubectl -n ndear get pods
145  kubectl -n ndear exec -it registry-5c649ddb85-9lpbm
146  kubectl -n ndear exec -it registry-5c649ddb85-9lpbm sh
147  kubectl -n ndear rollout restart deployment/registry
148  kubectl -n ndear logs -f  deployment/registry
149  cd
150  cd .ssh/
151  ls
152  exit
153  cd ndear/infra/
154  vi registry-service.yaml
155  kubectl -n ndear apply -f registry-service.yaml
156  kubectl -n ndear svc
157  kubectl -n ndear get svc
158  curl -v http://10.103.112.145:30127/health
159  curl -v http://10.103.112.145:30127
160  ifconfig
161  ipconfig
162  ip addr
163  curl -v http://10.4.0.6:30127
164  curl -v http://127.0.0.1:30127
165  netstat -na | grep LIST
166  sudo apt install net-tools
167  netstat -na | grep LIST
168  kubectl -n ndear get svc
169  kubectl -n ndear get nodes
170  kubectl -n ndear get nodes --wide
171  kubectl -n ndear get nodes -o wide
172  curl http://192.168.49.2
173  curl http://192.168.49.2:30127
174  curl http://192.168.49.2:30127/health
175  ls
176  vi claims-deployment.yaml
177  less claims-deployment.yaml
178  kubectl -n ndear get svc
179  vi claims-deployment.yaml
180  kubectl -n ndear -f claims-deployment.yaml
181  kubectl -n ndear apply -f claims-deployment.yaml
182  kubectl -n ndear apply logs -f deployment/claims
183  kubectl -n ndear logs -f deployment/claims
184  kubectl -n ndear get pods
185  kubectl -n ndear logs -f claim-ms-7fb7b6dfd4-ljp52
186  vi claims-service.yaml
187  kubectl -n ndear  apply -f claims-service.yaml
188  kubectl -n ndear get pods
189  kubectl -n ndear get svc
190  vi claims-deployment.yaml
191  vi registry--deployment.yaml
192  ls
193  vi registry-deployment.yaml
194  history
195  curl http://192.168.49.2:30128
196  kubectl -n ndear apply  -f registry-deployment.yaml
197  uname -a
198  cat /etc/*release
199  psql
200  history
201  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
202  fg
203  kubectl port-forward --namespace default svc/my-release-postgresql 5432:5432 &
204  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
205  $POSTGRES_PASSWORD
206  echo $POSTGRES_PASSWORD
207  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U opensaber -d postgres -p 5432
208  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
209  export POSTGRES_PASSWORD=$(kubectl get secret --namespace default my-release-postgresql -o jsonpath="{.data.postgresql-password}" | base64 --decode)
210  PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
211  vi keycloak-deployment.yaml
212  kubectl -n ndear apply -f keycloak-deployment.yaml
213  vi keycloak-service.yaml
214  kubectl -n ndear apply -f keycloak-service.yaml
215  kubectl -n ndear get pods
216  kubectl -n ndear log -f keycloak-service-58584fdbb4-99vlg
217  kubectl -n ndear logs -f keycloak-service-58584fdbb4-99vlg
218  kubectl -n ndear get svc