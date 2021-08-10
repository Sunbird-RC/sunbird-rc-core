 # OpenSABER Registry and Credentials

![Build](https://github.com/Sunbird-RC/opensaber-rc/actions/workflows/maven.yml/badge.svg)

Open Software for Rapidly Building Electronic Registries, shortly called OpenSABER-RC, is an open source software stack that enables running electronic registries with minimal effort.

Registry is a shared digital infrastructure which enables authorized data repositories to publish appropriate data and metadata about a user/entity along with the link to the repository in a digitally signed form. It allows data owners to provide authorized access to other users/entities in controlled manner for digital verification and usage.

## Getting started
### Test run with docker-compose
```shell script
wget https://raw.githubusercontent.com/Sunbird-RC/opensaber-rc/main/docker-compose.yml
docker-compose up -d
docker-compose ps
#wait for all services to start
curl -v http://localhost:8081/health
```
### Running with Docker
1. Customize the schema
        Refer to the example [here](../blob/main/docs/example/simple.json)
        ```wget https://raw.githubusercontent.com/Sunbird-RC/opensaber-rc/main/docs/example/simple.json```
2. Configure database type and detail
    * You can use postgres and prepare database using following command
        ```sql
      create database registry;
      create role registry identified by password('complexPassword') 
      grant all privileges  on database registry to registry;
        ``` 
3. Build docker image with custom schema
    ```shell script
   make   
   ```
4. start the docker instance.
    ```shell script
    docker -p 8081:8081 -v<schema_dir>:/home/opensaber/config/public/_schemas \
   -e connectionInfo_uri="jdbc:postgresql://localhost:5432/registry" \
   run your-registry 
    ```
5. Test the api
    ```shell script
    curl -v http://localhost:8081/health
    ```
## Demo
> Coming soon
    
## Dependencies
* Postgres or Cassandra as datastore
* Elastic search to enable search across attributes
* Keycloak to manage authorization and authentication

## Design principles
* Minimalism and decentralized
* Accountability and Non-repudiability
* Empowerment & Self-maintainability
* Security & Consented Access
* Universal Access & Open APIs 

## Architectural criteria
* Open API
* Standard JSON-LD based schema
* Production ready out of the box
* Customizable by configuration and extentions
* Scalable out of the box


## Examples:
* [Blood donor registry example schema](../blob/main/docs/example/simple.json)
* [Places schema](../blob/main/docs/example/place.json)




    
