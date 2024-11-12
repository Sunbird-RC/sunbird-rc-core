# Direct dependency analysis of sunbird-rc-registries

Parent project is **Open Software for Building Electronic Registries**    

## Summary 
There are 26 subprojects in the parent project with a total of 181 dependencies. 
There are 30 pom.xml files. 
The mismatch between the subproject count and dependency count that needs analysis. 

The subprojects are:

1. [pojos](#pojos)                                                           
2. [middleware commons](#middleware-commons)                                                 
3. [middleware-bom](#middleware-bom)                                                     
      1. [registry-middleware](#middleware-bom--registry-middleware)
      2. [Authorization](#middleware-bom--authorization)                                                      
      3. [Validation](#middleware-bom--validation)
      4. [Identity Provider](#middleware-bom--identity-provider)
      5. [keycloak](#middleware-bom--keycloak)
      6. [workflow](#middleware-bom--workflow)
      7. [auth0](#middleware-bom--auth0)
      8. [generic-iam](#middleware-bom--generic-iam)
4. [validators](#validators)                                                         
5. [Json based Validation](#json-based-validation)                                              
6. [JSON schema based validation](#json-schema-based-validation)                                       
7. [registry-interceptors](#registry-interceptors)                                              
8. [Elastic-Search](#elastic-search)                                                     
9. [sunbird-actor](#sunbird-actor)                                                      
10. [sunbirdrc-actors](#sunbirdrc-actors)                                                   
11. [plugins](#plugins)                                                            
12. [divoc-external-plugin](#divoc-external-plugin)                                              
13. [mosip-external-plugin](#mosip-external-plugin)                                              
14. [sample-external-plugin-2](#sample-external-plugin-2)                                           
15. [view-templates](#view-templates)                                                     
16. [registry](#registry)                                                           
17. [claim](#claim)                                                              
18. [apitest](#apitest)            

## Sub Project Dependencies
| Subproject                           | Dependency Count |
|--------------------------------------|------------------|
| pojos                                | 7                |
| middleware commons                   | 8                |
| middleware-bom > registry-middleware | 1                |
| middleware-bom > Authorization       | 18               |
| middleware-bom > validation          | 3                |
| middleware-bom > Identity Provider   | 1                |
| middleware-bom > keycloak            | 10               |
| middleware-bom > workflow            | 7                |
| middleware-bom > auth0               | 10               |
| middleware-bom > generic-iam         | 9                |
| validators                           | 3                |
| Json based Validation                | 3                |
| JSON schema based validation         | 4                |
| registry-interceptors                | 6                |
| Elastic-Search                       | 7                |
| sunbird-actor                        | 3                |
| sunbirdrc-actors                     | 3                |
| plugins                              | 2                |
| divoc-external-plugin                | 2                |
| mosip-external-plugin                | 2                |
| sample-external-plugin-2             | 2                |
| view-templates                       | 7                |
| registry                             | 48               |
| claim                                | 14               |
| apitest                              | 1                |
| **Total**                            | **181**          |
                                                                           

### pojos
| Serial No | Group ID                 | Artifact ID             | Type | Version       | Scope   | Module                          | Selected |
|-----------|--------------------------|-------------------------|------|---------------|---------|---------------------------------|----------|
| 1         | com.google.code.gson     | gson                    | jar  | 2.8.2         | compile | gson (auto)                     | [ ]      |
| 2         | commons-io               | commons-io              | jar  | 2.6           | compile | org.apache.commons.io [auto]    | [ ]      |
| 3         | junit                    | junit                   | jar  | 3.8.1         | test    | junit (auto)                    | [ ]      |
| 4         | org.apache.commons       | commons-lang3           | jar  | 3.12.0        | compile | org.apache.commons.lang3 [auto] | [ ]      |
| 5         | org.perf4j               | perf4j                  | jar  | 0.9.16        | compile | perf4j (auto)                   | [ ]      |
| 6         | org.projectlombok        | lombok                  | jar  | 1.18.20       | compile | lombok                          | [ ]      |
| 7         | org.springframework.boot | spring-boot-starter-web | jar  | 2.0.1.RELEASE | compile | spring.boot.starter.web [auto]  | [ ]      |

### middleware commons

| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                        | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|-------------------------------|----------|
| 1         | com.github.jsonld-java        | jsonld-java                   | jar   | 0.11.1         | compile | jsonld.java (auto)            | [ ]      |
| 2         | org.springframework           | spring-context                | jar   | 5.0.2.RELEASE  | compile | spring.context [auto]         | [ ]      |
| 3         | org.glassfish                 | javax.json                    | jar   | 1.1.4          | compile | java.json [auto]              | [ ]      |
| 4         | dev.sunbirdrc                 | pojos                         | jar   | 2.0.3          | compile | pojos (auto)                  | [ ]      |
| 5         | com.flipkart.zjsonpatch       | zjsonpatch                    | jar   | 0.4.6          | compile | zjsonpatch (auto)             | [ ]      |
| 6         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                  | [ ]      |
| 7         | org.springframework           | spring-beans                  | jar   | 5.0.13.RELEASE | compile | spring.beans [auto]           | [ ]      |
| 8         | com.jayway.jsonpath           | json-path                     | jar   | 2.4.0          | compile | json.path (auto)              | [ ]      |

### middleware-bom

#### middleware-bom > registry-middleware
| Serial No | Group ID      | Artifact ID | Type | Version | Scope   | Module | Selected |
|-----------|---------------|-------------|------|---------|---------|--------|----------|
| 1         | dev.sunbirdrc | pojos       | jar  | 2.0.3   | compile | pojos  | [ ]      |

#### middleware-bom > Authorization
| Serial No | Group ID                                | Artifact ID                            | Type | Version        | Scope              | Module                                        | Selected |
|-----------|-----------------------------------------|----------------------------------------|------|----------------|--------------------|-----------------------------------------------|----------|
| 1         | com.fasterxml.jackson.core              | jackson-databind                       | jar  | 2.10.0         | compile            | com.fasterxml.jackson.databind                | [ ]      |
| 2         | dev.sunbirdrc                           | middleware-commons                     | jar  | 2.0.3          | compile            | middleware.commons (auto)                     | [ ]      |
| 3         | dev.sunbirdrc                           | pojos                                  | jar  | 2.0.3          | compile            | pojos (auto)                                  | [ ]      |
| 4         | io.jsonwebtoken                         | jjwt                                   | jar  | 0.9.0          | compile            | jjwt (auto)                                   | [ ]      |
| 5         | jakarta.validation                      | jakarta.validation-api                 | jar  | 2.0.2          | compile            | java.validation [auto]                        | [ ]      |
| 6         | junit                                   | junit                                  | jar  | 4.12           | test               | junit (auto)                                  | [ ]      |
| 7         | org.apache.commons                      | commons-lang3                          | jar  | 3.0            | compile            | commons.lang3 (auto)                          | [ ]      |
| 8         | org.keycloak                            | keycloak-admin-client                  | jar  | 3.2.0.Final    | compile            | keycloak.admin.client (auto)                  | [ ]      |
| 9         | org.mockito                             | mockito-core                           | jar  | 4.2.0          | test               | org.mockito [auto]                            | [ ]      |
| 10        | org.powermock                           | powermock-api-mockito2                 | jar  | 2.0.9          | test               | powermock.api.mockito2 (auto)                 | [ ]      |
| 11        | org.powermock                           | powermock-module-junit4                | jar  | 2.0.9          | test               | powermock.module.junit4 (auto)                | [ ]      |
| 12        | org.springframework.boot                | spring-boot-configuration-processor    | jar  | 2.3.12.RELEASE | compile (optional) | spring.boot.configuration.processor [auto]    | [ ]      |
| 13        | org.springframework.boot                | spring-boot-starter-security           | jar  | 2.3.12.RELEASE | compile            | spring.boot.starter.security [auto]           | [ ]      |
| 14        | org.springframework.boot                | spring-boot-starter-web                | jar  | 2.3.12.RELEASE | compile            | spring.boot.starter.web [auto]                | [ ]      |
| 15        | org.springframework.boot                | spring-boot-test                       | jar  | 2.0.0.RELEASE  | test               | spring.boot.test [auto]                       | [ ]      |
| 16        | org.springframework.security            | spring-security-oauth2-jose            | jar  | 5.3.9.RELEASE  | compile            | spring.security.oauth2.jose [auto]            | [ ]      |
| 17        | org.springframework.security            | spring-security-oauth2-resource-server | jar  | 5.3.9.RELEASE  | compile            | spring.security.oauth2.resource.server [auto] | [ ]      |
| 18        | org.springframework.security.oauth.boot | spring-security-oauth2-autoconfigure   | jar  | 2.3.1.RELEASE  | compile            | spring.security.oauth2.autoconfigure [auto]   | [ ]      |

#### middleware-bom > Validation
| Serial No | Group ID      | Artifact ID        | Type | Version | Scope   | Module                    | Selected |
|-----------|---------------|--------------------|------|---------|---------|---------------------------|----------|
| 1         | dev.sunbirdrc | middleware-commons | jar  | 2.0.3   | compile | middleware.commons (auto) | [ ]      |
| 2         | junit         | junit              | jar  | 4.12    | test    | junit (auto)              | [ ]      |
| 3         | dev.sunbirdrc | pojos              | jar  | 2.0.3   | compile | pojos (auto)              | [ ]      |

#### middleware-bom > Identity Provider
| Serial No | Group ID      | Artifact ID | Type | Version | Scope   | Module | Selected |
|-----------|---------------|-------------|------|---------|---------|--------|----------|
| 1         | dev.sunbirdrc | pojos       | jar  | 2.0.3   | compile | pojos  | [ ]      |

#### middleware-bom > keycloak
| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                          | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|---------------------------------|----------|
| 1         | org.keycloak                  | keycloak-admin-client         | jar   | 14.0.0         | compile | keycloak.admin.client (auto)    | [ ]      |
| 2         | org.springframework           | spring-context                | jar   | 5.0.2.RELEASE  | compile | spring.context [auto]           | [ ]      |
| 3         | org.springframework           | spring-web                    | jar   | 5.0.2.RELEASE  | compile | spring.web [auto]               | [ ]      |
| 4         | org.slf4j                     | slf4j-api                     | jar   | 1.7.32         | compile | org.slf4j [auto]                | [ ]      |
| 5         | org.springframework.boot      | spring-boot-starter-test      | jar   | 2.5.0          | test    | spring.boot.starter.test [auto] | [ ]      |
| 6         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                    | [ ]      |
| 7         | org.springframework           | spring-test                   | jar   | 5.3.9          | test    | spring.test [auto]              | [ ]      |
| 8         | dev.sunbirdrc                 | pojos                         | jar   | 2.0.3          | compile | pojos (auto)                    | [ ]      |
| 9         | dev.sunbirdrc                 | middleware-commons            | jar   | 2.0.3          | compile | middleware.commons (auto)       | [ ]      |
| 10        | dev.sunbirdrc                 | identity-provider             | jar   | 2.0.3          | compile | identity.provider (auto)        | [ ]      |

#### middleware-bom > workflow

| Serial No | Group ID      | Artifact ID           | Type | Version      | Scope   | Module                           | Selected |
|-----------|---------------|-----------------------|------|--------------|---------|----------------------------------|----------|
| 1         | dev.sunbirdrc | pojos                 | jar  | 2.0.3        | compile | pojos (auto)                     | [ ]      |
| 2         | dev.sunbirdrc | middleware-commons    | jar  | 2.0.3        | compile | middleware.commons (auto)        | [ ]      |
| 3         | junit         | junit                 | jar  | 4.12         | test    | junit (auto)                     | [ ]      |
| 4         | org.drools    | drools-core           | jar  | 7.49.0.Final | compile | org.drools.core [auto]           | [ ]      |
| 5         | org.drools    | drools-compiler       | jar  | 7.49.0.Final | compile | org.drools.compiler [auto]       | [ ]      |
| 6         | org.drools    | drools-decisiontables | jar  | 7.49.0.Final | compile | org.drools.decisiontables [auto] | [ ]      |
| 7         | dev.sunbirdrc | identity-provider     | jar  | 2.0.3        | compile | identity.provider (auto)         | [ ]      |

#### middleware-bom > auth0
 
| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                          | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|---------------------------------|----------|
| 1         | com.auth0                     | auth0                         | jar   | 2.3.0          | compile | auth0 (auto)                    | [ ]      |
| 2         | org.springframework           | spring-context                | jar   | 5.0.2.RELEASE  | compile | spring.context [auto]           | [ ]      |
| 3         | org.springframework           | spring-web                    | jar   | 5.0.2.RELEASE  | compile | spring.web [auto]               | [ ]      |
| 4         | org.slf4j                     | slf4j-api                     | jar   | 1.7.32         | compile | org.slf4j [auto]                | [ ]      |
| 5         | org.springframework.boot      | spring-boot-starter-test      | jar   | 2.5.0          | test    | spring.boot.starter.test [auto] | [ ]      |
| 6         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                    | [ ]      |
| 7         | org.springframework           | spring-test                   | jar   | 5.3.9          | test    | spring.test [auto]              | [ ]      |
| 8         | dev.sunbirdrc                 | pojos                         | jar   | 2.0.3          | compile | pojos (auto)                    | [ ]      |
| 9         | dev.sunbirdrc                 | middleware-commons            | jar   | 2.0.3          | compile | middleware.commons (auto)       | [ ]      |
| 10        | dev.sunbirdrc                 | identity-provider             | jar   | 2.0.3          | compile | identity.provider (auto)        | [ ]      |

#### middleware-bom > generic-iam
| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                          | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|---------------------------------|----------|
| 1         | org.springframework           | spring-context                | jar   | 5.0.2.RELEASE  | compile | spring.context [auto]           | [ ]      |
| 2         | org.springframework           | spring-web                    | jar   | 5.0.2.RELEASE  | compile | spring.web [auto]               | [ ]      |
| 3         | org.slf4j                     | slf4j-api                     | jar   | 1.7.32         | compile | org.slf4j [auto]                | [ ]      |
| 4         | org.springframework.boot      | spring-boot-starter-test      | jar   | 2.5.0          | test    | spring.boot.starter.test [auto] | [ ]      |
| 5         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                    | [ ]      |
| 6         | org.springframework           | spring-test                   | jar   | 5.3.9          | test    | spring.test [auto]              | [ ]      |
| 7         | dev.sunbirdrc                 | pojos                         | jar   | 2.0.3          | compile | pojos (auto)                    | [ ]      |
| 8         | dev.sunbirdrc                 | middleware-commons            | jar   | 2.0.3          | compile | middleware.commons (auto)       | [ ]      |
| 9         | dev.sunbirdrc                 | identity-provider             | jar   | 2.0.3          | compile | identity.provider (auto)        | [ ]      |

### validators

| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                          | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|---------------------------------|----------|
| 1         | dev.sunbirdrc                 | middleware-commons            | jar   | 2.0.3          | compile | middleware.commons (auto)       | [ ]      |
| 2         | dev.sunbirdrc.middleware      | validation                    | jar   | 2.0.3          | compile | validation (auto)               | [ ]      |
| 3         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                    | [ ]      |

### Json based Validation
| Serial No | Group ID                 | Artifact ID        | Type | Version | Scope   | Module             | Selected |
|-----------|--------------------------|--------------------|------|---------|---------|--------------------|----------|
| 1         | dev.sunbirdrc            | middleware-commons | jar  | 2.0.3   | compile | middleware.commons | [ ]      |
| 2         | dev.sunbirdrc.middleware | validation         | jar  | 2.0.3   | compile | validation         | [ ]      |
| 3         | junit                    | junit              | jar  | 4.12    | test    | junit              | [ ]      |

### JSON schema based validation
| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                          | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|---------------------------------|----------|
| 1         | com.github.everit-org         | org.everit.json.schema        | jar   | 1.12.2         | compile | org.everit.json.schema (auto)   | [ ]      |
| 2         | junit                         | junit                         | jar   | 4.12           | test    | junit (auto)                    | [ ]      |
| 3         | dev.sunbirdrc                 | middleware-commons            | jar   | 2.0.3          | compile | middleware.commons (auto)       | [ ]      |
| 4         | dev.sunbirdrc.middleware      | validation                    | jar   | 2.0.3          | compile | validation (auto)               | [ ]      |

### registry-interceptors
| Serial No | Group ID                      | Artifact ID                   | Type  | Version        | Scope   | Module                | Selected |
|-----------|-------------------------------|-------------------------------|-------|----------------|---------|-----------------------|----------|
| 1         | org.apache.commons            | commons-lang3                 | jar   | 3.4            | compile | commons.lang3 (auto)  | [ ]      |
| 2         | dev.sunbirdrc                 | authorization                 | jar   | 2.0.3          | compile | authorization (auto)  | [ ]      |
| 3         | com.google.code.gson          | gson                          | jar   | 2.8.2          | compile | gson (auto)           | [ ]      |
| 4         | dev.sunbirdrc                 | pojos                         | jar   | 2.0.3          | compile | pojos (auto)          | [ ]      |
| 5         | org.springframework           | spring-context                | jar   | 5.0.2.RELEASE  | compile | spring.context [auto] | [ ]      |
| 6         | org.springframework           | spring-web                    | jar   | 5.0.2.RELEASE  | compile | spring.web [auto]     | [ ]      |


### Elastic-Search

| Serial No | Group ID                                      | Artifact ID                           | Type  | Version        | Scope   | Module                                    | Selected |
|-----------|-----------------------------------------------|---------------------------------------|-------|----------------|---------|-------------------------------------------|----------|
| 1         | org.elasticsearch                             | elasticsearch                         | jar   | 6.6.0          | compile |                                           | [ ]      |
| 2         | org.elasticsearch.client                      | elasticsearch-rest-high-level-client  | jar   | 6.6.0          | compile | elasticsearch.rest.high.level.client      | [ ]      |
| 3         | org.elasticsearch.client                      | elasticsearch-rest-client             | jar   | 6.6.0          | compile | elasticsearch.rest.client                 | [ ]      |
| 4         | dev.sunbirdrc                                 | pojos                                 | jar   | 2.0.3          | compile | pojos                                     | [ ]      |
| 5         | dev.sunbirdrc                                 | middleware-commons                    | jar   | 2.0.3          | compile | middleware.commons                        | [ ]      |
| 6         | org.springframework.retry                     | spring-retry                          | jar   | 1.2.2.RELEASE  | compile | spring.retry                              | [ ]      |
| 7         | org.apache.commons                            | commons-lang3                         | jar   | 3.4            | compile | commons.lang3                             | [ ]      |

### sunbird-actor
| Serial No | Group ID            | Artifact ID      | Type | Version  | Scope   | Module        | Selected |
|-----------|---------------------|------------------|------|----------|---------|---------------|----------|
| 1         | com.typesafe.akka   | akka-remote_2.12 | jar  | 2.6.0-M2 | compile | akka.remote   | [ ]      |
| 2         | com.google.protobuf | protobuf-java    | jar  | 3.6.1    | compile | protobuf.java | [ ]      |
| 3         | org.apache.commons  | commons-lang3    | jar  | 3.0      | compile | commons.lang3 | [ ]      |

### sunbirdrc-actors
| Serial No | Group ID             | Artifact ID    | Type | Version       | Scope   | Module         | Selected |
|-----------|----------------------|----------------|------|---------------|---------|----------------|----------|
| 1         | org.sunbird.akka     | sunbird-actor  | jar  | 1.0.0         | compile | sunbird.actor  | [ ]      |
| 2         | dev.sunbirdrc        | elastic-search | jar  | 2.0.3         | compile | elastic.search | [ ]      |
| 3         | com.squareup.okhttp3 | okhttp         | jar  | 5.0.0-alpha.2 | compile | okhttp3 [auto] | [ ]      |
### plugins
| Serial No | Group ID         | Artifact ID      | Type | Version | Scope   | Module           | Selected |
|-----------|------------------|------------------|------|---------|---------|------------------|----------|
| 1         | org.sunbird.akka | sunbird-actor    | jar  | 1.0.0   | compile | sunbird.actor    | [ ]      |
| 2         | dev.sunbirdrc    | sunbirdrc-actors | jar  | 2.0.3   | compile | sunbirdrc.actors | [ ]      |
### divoc-external-plugin
| Serial No | Group ID         | Artifact ID      | Type | Version | Scope   | Module           | Selected |
|-----------|------------------|------------------|------|---------|---------|------------------|----------|
| 1         | org.sunbird.akka | sunbird-actor    | jar  | 1.0.0   | compile | sunbird.actor    | [ ]      |
| 2         | dev.sunbirdrc    | sunbirdrc-actors | jar  | 2.0.3   | compile | sunbirdrc.actors | [ ]      |

### mosip-external-plugin
| Serial No | Group ID             | Artifact ID      | Type | Version  | Scope   | Module           | Selected |
|-----------|----------------------|------------------|------|----------|---------|------------------|----------|
| 1         | org.sunbird.akka     | sunbird-actor    | jar  | 1.0.0    | compile | sunbird.actor    | [ ]      |
| 2         | dev.sunbirdrc        | sunbirdrc-actors | jar  | 2.0.3    | compile | sunbirdrc.actors | [ ]      |

### sample-external-plugin-2
| Serial No | Group ID             | Artifact ID      | Type | Version  | Scope   | Module           | Selected |
|-----------|----------------------|------------------|------|----------|---------|------------------|----------|
| 1         | org.sunbird.akka     | sunbird-actor    | jar  | 1.0.0    | compile | sunbird.actor    | [ ]      |
| 2         | dev.sunbirdrc        | sunbirdrc-actors | jar  | 2.0.3    | compile | sunbirdrc.actors | [ ]      |

### view-templates
| Serial No | Group ID                   | Artifact ID      | Type | Version | Scope    | Module                         | Selected |
|-----------|----------------------------|------------------|------|---------|----------|--------------------------------|----------|
| 1         | com.fasterxml.jackson.core | jackson-databind | jar  | 2.12.0  | compile  | com.fasterxml.jackson.databind | [ ]      |
| 2         | org.apache.commons         | commons-jexl     | jar  | 2.1.1   | compile  | commons.jexl                   | [ ]      |
| 3         | org.apache.commons         | commons-lang3    | jar  | 3.4     | compile  | commons.lang3                  | [ ]      |
| 4         | junit                      | junit            | jar  | 4.12    | test     | junit                          | [ ]      |
| 5         | org.mockito                | mockito-core     | jar  | 2.12.0  | test     | org.mockito                    | [ ]      |
| 6         | com.jayway.jsonpath        | json-path        | jar  | 2.4.0   | compile  | json.path                      | [ ]      |
| 7         | org.projectlombok          | lombok           | jar  | 1.18.20 | provided | lombok                         | [ ]      |
### registry
| Serial No | Group ID                     | Artifact ID                      | Type | Version        | Scope    | Module                                  | Selected |
|-----------|------------------------------|----------------------------------|------|----------------|----------|-----------------------------------------|----------|
| 1         | com.github.jknack            | handlebars                       | jar  | 4.3.1          | compile  | handlebars (auto)                       | [ ]      |
| 2         | com.google.code.gson         | gson                             | jar  | 2.8.2          | compile  | gson (auto)                             | [ ]      |
| 3         | com.google.guava             | guava                            | jar  | 23.0           | compile  | guava (auto)                            | [ ]      |
| 4         | com.googlecode.junit-toolbox | junit-toolbox                    | jar  | 1.5            | test     | junit.toolbox (auto)                    | [ ]      |
| 5         | com.intuit.karate            | karate-junit5                    | jar  | 1.0.1          | test     | karate.junit5 (auto)                    | [ ]      |
| 6         | com.microsoft.sqlserver      | mssql-jdbc                       | jar  | 12.6.1.jre8    | compile  | mssql.jdbc (auto)                       | [ ]      |
| 7         | com.orientechnologies        | orientdb-gremlin                 | jar  | 3.0.0m2        | compile  | orientdb.gremlin (auto)                 | [ ]      |
| 8         | com.squareup.okhttp3         | okhttp                           | jar  | 4.8.1          | compile  | okhttp3 (auto)                          | [ ]      |
| 9         | com.steelbridgelabs.oss      | neo4j-gremlin-bolt               | jar  | 0.2.27         | compile  | neo4j.gremlin.bolt (auto)               | [ ]      |
| 10        | commons-io                   | commons-io                       | jar  | 2.6            | compile  | org.apache.commons.io (auto)            | [ ]      |
| 11        | dev.sunbirdrc                | auth0                            | jar  | 2.0.3          | compile  | auth0 (auto)                            | [ ]      |
| 12        | dev.sunbirdrc                | divoc-external-plugin            | jar  | 2.0.3          | compile  | divoc.external.plugin (auto)            | [ ]      |
| 13        | dev.sunbirdrc                | elastic-search                   | jar  | 2.0.3          | compile  | elastic.search (auto)                   | [ ]      |
| 14        | dev.sunbirdrc                | generic-iam                      | jar  | 2.0.3          | compile  | generic.iam (auto)                      | [ ]      |
| 15        | dev.sunbirdrc                | identity-provider                | jar  | 2.0.3          | compile  | identity.provider (auto)                | [ ]      |
| 16        | dev.sunbirdrc                | jsonschemavalidator              | jar  | 2.0.3          | compile  | jsonschemavalidator (auto)              | [ ]      |
| 17        | dev.sunbirdrc                | keycloak                         | jar  | 2.0.3          | compile  | keycloak (auto)                         | [ ]      |
| 18        | dev.sunbirdrc                | mosip-external-plugin            | jar  | 2.0.3          | compile  | mosip.external.plugin (auto)            | [ ]      |
| 19        | dev.sunbirdrc                | registry-interceptor             | jar  | 2.0.3          | compile  | registry.interceptor (auto)             | [ ]      |
| 20        | dev.sunbirdrc                | sample-external-plugin-2         | jar  | 2.0.3          | compile  | sample.external.plugin-2 (auto)         | [ ]      |
| 21        | dev.sunbirdrc                | sunbirdrc-actors                 | jar  | 2.0.3          | compile  | sunbirdrc.actors (auto)                 | [ ]      |
| 22        | dev.sunbirdrc                | view-templates                   | jar  | 1.0.0          | compile  | view.templates (auto)                   | [ ]      |
| 23        | dev.sunbirdrc                | workflow                         | jar  | 2.0.3          | compile  | workflow (auto)                         | [ ]      |
| 24        | io.minio                     | minio                            | jar  | 8.3.0          | compile  | minio (auto)                            | [ ]      |
| 25        | io.swagger                   | swagger-core                     | jar  | 1.6.2          | compile  | swagger.core (auto)                     | [ ]      |
| 26        | javax.ws.rs                  | javax.ws.rs-api                  | jar  | 2.1.1          | compile  | java.ws.rs                              | [ ]      |
| 27        | junit                        | junit                            | jar  | 4.12           | test     | junit (auto)                            | [ ]      |
| 28        | org.apache.commons           | commons-text                     | jar  | 1.9            | compile  | org.apache.commons.text (auto)          | [ ]      |
| 29        | org.apache.httpcomponents    | httpclient-cache                 | jar  | 4.5.4          | compile  | httpclient.cache (auto)                 | [ ]      |
| 30        | org.apache.httpcomponents    | httpcore                         | jar  | 4.4.8          | compile  | httpcore (auto)                         | [ ]      |
| 31        | org.apache.tinkerpop         | gremlin-core                     | jar  | 3.3.3          | compile  | gremlin.core (auto)                     | [ ]      |
| 32        | org.apache.tinkerpop         | neo4j-gremlin                    | jar  | 3.3.3          | compile  | neo4j.gremlin (auto)                    | [ ]      |
| 33        | org.apache.tinkerpop         | tinkergraph-gremlin              | jar  | 3.3.3          | compile  | tinkergraph.gremlin (auto)              | [ ]      |
| 34        | org.codehaus.groovy          | groovy-all                       | jar  | 1.6-beta-2     | test     | groovy.all (auto)                       | [ ]      |
| 35        | org.janusgraph               | janusgraph-core                  | jar  | 0.3.1          | compile  | janusgraph.core (auto)                  | [ ]      |
| 36        | org.jboss.resteasy           | resteasy-client                  | jar  | 3.9.1.Final    | compile  | resteasy.client (auto)                  | [ ]      |
| 37        | org.jboss.resteasy           | resteasy-jackson2-provider       | jar  | 3.9.1.Final    | compile  | resteasy.jackson2.provider (auto)       | [ ]      |
| 38        | org.keycloak                 | keycloak-spring-security-adapter | jar  | 14.0.0         | compile  | keycloak.spring.security.adapter (auto) | [ ]      |
| 39        | org.neo4j                    | neo4j-tinkerpop-api-impl         | jar  | 0.7-3.2.3      | compile  | neo4j.tinkerpop.api.impl (auto)         | [ ]      |
| 40        | org.postgresql               | postgresql                       | jar  | 42.2.20        | compile  | org.postgresql.jdbc (auto)              | [ ]      |
| 41        | org.projectlombok            | lombok                           | jar  | 1.18.20        | provided | lombok                                  | [ ]      |
| 42        | org.springframework.boot     | spring-boot-starter-security     | jar  | 2.3.12.RELEASE | compile  | spring.boot.starter.security [auto]     | [ ]      |
| 43        | org.springframework.boot     | spring-boot-starter-test         | jar  | 2.3.12.RELEASE | test     | spring.boot.starter.test [auto]         | [ ]      |
| 44        | org.springframework.boot     | spring-boot-starter-validation   | jar  | 2.3.12.RELEASE | compile  | spring.boot.starter.validation [auto]   | [ ]      |
| 45        | org.springframework.boot     | spring-boot-starter-web          | jar  | 2.3.12.RELEASE | compile  | spring.boot.starter.web [auto]          | [ ]      |
| 46        | org.springframework.data     | spring-data-commons              | jar  | 2.3.9.RELEASE  | compile  | spring.data.commons [auto]              | [ ]      |
| 47        | org.springframework.kafka    | spring-kafka                     | jar  | 2.8.6          | compile  | spring.kafka [auto]                     | [ ]      |
| 48        | redis.clients                | jedis                            | jar  | 3.3.0          | compile  | jedis (auto)                            | [ ]      |
### claim
| Serial No | Group ID                 | Artifact ID                  | Type | Version    | Scope   | Module                              | Selected |
|-----------|--------------------------|------------------------------|------|------------|---------|-------------------------------------|----------|
| 1         | com.jayway.jsonpath      | json-path                    | jar  | 2.4.0      | compile | json.path (auto)                    | [ ]      |
| 2         | com.microsoft.sqlserver  | mssql-jdbc                   | jar  | 9.2.1.jre8 | runtime | mssql.jdbc (auto)                   | [ ]      |
| 3         | dev.sunbirdrc            | middleware-commons           | jar  | 2.0.3      | compile | middleware.commons (auto)           | [ ]      |
| 4         | dev.sunbirdrc            | pojos                        | jar  | 2.0.3      | compile | pojos (auto)                        | [ ]      |
| 5         | io.springfox             | springfox-boot-starter       | jar  | 3.0.0      | compile | springfox.boot.starter (auto)       | [ ]      |
| 6         | io.springfox             | springfox-swagger-ui         | jar  | 3.0.0      | compile | springfox.swagger.ui (auto)         | [ ]      |
| 7         | junit                    | junit                        | jar  | 4.12       | test    | junit (auto)                        | [ ]      |
| 8         | org.mockito              | mockito-core                 | jar  | 2.12.0     | test    | org.mockito [auto]                  | [ ]      |
| 9         | org.postgresql           | postgresql                   | jar  | 42.2.20    | compile | org.postgresql.jdbc (auto)          | [ ]      |
| 10        | org.springframework.boot | spring-boot-starter-actuator | jar  | 2.5.0      | compile | spring.boot.starter.actuator [auto] | [ ]      |
| 11        | org.springframework.boot | spring-boot-starter-data-jpa | jar  | 2.5.0      | compile | spring.boot.starter.data.jpa [auto] | [ ]      |
| 12        | org.springframework.boot | spring-boot-starter-test     | jar  | 2.5.0      | test    | spring.boot.starter.test [auto]     | [ ]      |
| 13        | org.springframework.boot | spring-boot-starter-web      | jar  | 2.5.0      | compile | spring.boot.starter.web [auto]      | [ ]      |
| 14        | org.springframework.boot | spring-boot-starter          | jar  | 2.5.0      | compile | spring.boot.starter [auto]          | [ ]      |
### apitest
| Serial No | Group ID                      | Artifact ID      | Type | Version  | Scope   | Module                  | Selected |
|-----------|-------------------------------|------------------|------|----------|---------|-------------------------|----------|
| 1         | com.intuit.karate             | karate-junit5    | jar  | 1.3.0    | test    | karate.junit5 (auto)    | [ ]      |
