# CCS ESourcing Integration (API)

## Introduction

The CCS ESourcing Integration API project provides integration and mapping between the CCS Salesforce and Jaggaer Sourcing platforms.

It "sits" between CCSs Salesforce CRM and CCSs Jaggaer eSourcing applications. The Salesforce and Jaggaer applications may call, via REST endpoints the Integration API and the Integration API will translate the call and the data as required before calling the other application. In some cases, the Integration API may poll one of the services for updated information and then call the other application.

The name "Integration API" is misnomer as this will consist of an Integration API and an implementation of the API in other words an Integration Service.

<p align="center">
  <img src="./docs/Simple Block Diagram.png"/>
</p>
<p align="center">
Simple Block Diagram
</p>


The APIs exposed and used by the application are all defined using OpenAPI definitions. These definitions are available as downloadable resources allowing ease of use with tools such as SmartBears Swagger UI.

<p align="center">
  <img src="./docs/Detailed Block Diagram.png"/>
</p>
<p align="center">
Detailed Block Diagram
</p>


## Prerequisites
The application can de developed and built on Windows, Linux or MacOS.

The application is written in Java and requires Java 11+. AdoptOpenJDK version jdk-11.0.7.10-hotspot was used for development.

The project is built and managed using Maven. Maven version 3.6.3 was used during development.

The project is written in Java and was developed using both Eclipse or IntelliJ IDEs

Recommended Eclipse installation

+ Eclipse 2020-09
+ Bndtools plugin (so can open jars and wars files)
+ SpotBugs plugin
+ EasyShell plugin (so can open Windows Explorer and/or Command Prompt)

Recommended IntelliJ installation:

+ IntelliJ Idea 2020.3
+ Maven Helper plugin
+ SonarLint plugin
+ Swagger plugin
+ Google Java Format plugin

Eclipse and IntelliJ must be configured to use same source code formatting rules to ensure that no changes/diffs are introduced due to IDE reformatting. 

Java code formatting should follow the [Google Java Code style](https://google.github.io/styleguide/javaguide.html) using the [supported tooling](https://github.com/google/google-java-format) if required. The Eclipse the coding style configuration is available here [eclipse-java-google-style.xml](./dev/resources/eclipse-java-google-style.xml)

Static analysis is performed using SpotBugs in the maven build.

The Cloud Foundry CLI tools can be installed according to the Cloud Foundry CLI [installation instructions](https://docs.cloudfoundry.org/cf-cli/install-go-cli.html). At the time of writing version 7.1.0 was the latest available.

## How to get started
The project is a multi-module maven project. The top-level project also acts as a parent module for the sub-modules and is used to define common dependencies and configuration. The sub-modules are

+ integration-server-api
  + an autogenerated server implementation of the OpenAPI definition of the application
+ jaggaer-client-api
  + an autogenerated client built from an OpenAPI definition of a subset (only those endpoints required by this application) of the Jaggaer API
+ jaggaer-mocksvr-api
  + an autogenerated mock service built from the OpenAPI definition of the subset of the Jaggaer API. Useful for providing a mock set of endpoints for ad-hoc/informal testing
+ integration-app
  + the Spring Boot based java application

The application is built using maven. There are a number of unit and integration level tests which can be run (or not) as required.

To build the project and run the unit tests:

```powershell
mvn clean package
```

To build the project and run the unit and integration tests:

```powershell
mvn clean verify
```

To run the application locally

```powershell
java -Dspring.profiles.active=local -jar .\integration-app\target\integration-app-1.0.0-SNAPSHOT.jar
```

(The application has a number of Spring profiles defined, more on these later, but to run locally on a development machine the *local* profile should be selected).

The application will start and expose an endpoint. If run with default settings then this will be accessible on http://localhost:8080/

The application has a large number of configuration parameters and options available (more on these later). As this is a Spring Boot application the configuration can be defined in an application.properties file in a suitable location, as parameters passed in on the command line using the -D switch or as environment variables. How best to do this will be dependent upon the environment and the number of properties being set. Being a Spring Boot application that uses [Relaxed Binding](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-relaxed-binding) the parameters can be specified in a number of ways. For example the Rollbar Access Token property can be set as

+ a kebab-case property rollbar.access-token
+ a camel case property rollbar.accessToken
+ an environment variable ROLLBAR_ACCESSTOKEN

## Branching, deployment and profiles

The project uses the Gitflow workflow for its branching strategy. This consists of a *main* branch, a *develop* branch and numerous ephemeral *feature* branches. This won't de described here as there is lots of information available describing the Gitflow workflow elsewhere, such as on the [Atlassian Bitbucket documentation](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow). 

In addition to the Gitflow branches there are additional deployment branches which are used as part of the DevOps CI/CD process. 

Within the GOV.UK PaaS Cloud Foundry [Organisation](https://docs.cloudfoundry.org/concepts/roles.html#Org) for the project there are a number of [Spaces](https://docs.cloudfoundry.org/concepts/roles.html#Spaces). Each of these Spaces represents a deployment environment. The branching in the GitHub repository mirrors this with a *deploy/space* branch for each environment. Travis CI is configured to detect changes to these branches and then build and deploy automatically into the corresponding Space.

The is taken further with the project contriving to create [Spring Profile](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-profiles)s which also match the Spaces and the deployment branches. The Spring Profile is then used to set the more static configuration suitable for the particular deployment environment. As an example the *local* profile configuration is defined in the [application-local.properties](./integration-app/src/main/resources/application-local.properties) file.

| Spring Profile | Cloud Foundry Space | Git Branch     | Travis Manifest  |                                                              |
| -------------- | ------------------- | -------------- | ---------------- | ------------------------------------------------------------ |
| local          | none                | none           | none             | Used for local development testing                           |
| dev            | Dev                 | none           | manifest-dev     | Used for manual development testing, no CI/CD, manual pushes to Dev |
| sandbox        | sandbox             | deploy/sandbox | manifest-sandbox | Shared development environment, CI/CD when changes pushed to deploy/sandbox |
| test           | Test                | deploy/test    | manifest-test    | Shared test environment, CI/CD when changes pushed to deploy/test |
| uat            | UAT                 | none           | none             | Shared UAT environment, no CI/CD configured, not used        |

## How to configure and deploy the project

The application is a Spring Boot Java application that is designed to be built using [Travis CI](https://travis-ci.com/) and then deployed into a [GOV.UK PaaS](https://login.london.cloud.service.gov.uk/) / Cloud Foundry platform but it can be deployed and run as a standalone application. As such it contains

+ .travis.yml
  + Travis CI configuration file for CI/CD integration
+ manifest.yml
  + GOV.UK PaaS / CloudFoundry manifest file

These files will be environment specific with properties set for a particular deployment environment and will differ across deployment branches.

### Configuration properties

All of the ESourcing application specific application properties that can be set are listed and described in the [application.properties](./integration-app/src/main/resources/application.properties) file. Additionally Spring Boot has a huge number of properties that are documented on the [Common Application properties](https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html) page.

Given that most of these properties have suitable default values set for each of the environments the following lists properties that are of interest that may need to be set.

| Property                                                            | Environment variable                                               | Type          | Description                                                                                                       |
| ------------------------------------------------------------------- | ------------------------------------------------------------------ | ------------- | ----------------------------------------------------------------------------------------------------------------- |
| ccs.esourcing.ip-allow-list                                         | CCS_ESOURCING_IPALLOWLIST                                          | string        | Comma separated list of ipv4 or ipv6 addresses from which requests are allowed from, empty list then no filtering |
| ccs.esourcing.api-key-header                                        | CCS_ESOURCING_APIKEYHEADER                                         | string        | HTTP header to be used for API Key authentication                                                                 |
| ccs.esourcing.api-keys                                              | CCS_ESOURCING_APIKEYS                                              | string        | Comma separated list of API keys used for "authenticating" requests                                               |
| rollbar.enabled                                                     | ROLLBAR_ENABLED                                                    | true \| false | Enable Rollbar integration                                                                                        |
| rollbar.access-token                                                | ROLLBAR_ACCESSTOKEN                                                | string        | The access token for the Rollbar account/project                                                                  |
| rollbar.environment                                                 | ROLLBAR_ENVIRONMENT                                                | string        | Value to use for the Rollbar environment attribute                                                                |
| rollbar.framework                                                   | ROLLBAR_FRAMEWORK                                                  | string        | Value to use for the Rollbar framework attribute                                                                  |
| rollbar.endpoint                                                    | ROLLBAR_ENDPOINT                                                   | string        | Can be used to override Rollbar URL endpoint                                                                      |
| spring.security.oauth2.client.provider.jaggaer.token-uri            | SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_JAGGAER_TOKENURI            | string        | Jaggaer Token API URL                                                                                             |
| spring.security.oauth2.client.registration.jaggaer.client-id        | SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_CLIENTID        | string        | Jaggaer Client ID                                                                                                 |
| spring.security.oauth2.client.registration.jaggaer.client-secret    | SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_CLIENTSECRET    | string        | Jaggaer Client Secret                                                                                             |
| ccs.esourcing.jaggaer.client-url                                    | CCS_ESOURCING_JAGGAER_CLIENTURL                                    | string        | The URL of the Jaggaer API                                                                                        |
| ccs.esourcing.jaggaer.default.buyer-company-id                      | CCS_ESOURCING_JAGGAER_DEFAULT_BUYERCOMPANYID                       | string        | Default Jaggaer Buyer Companny ID                                                                                 | 
| spring.security.oauth2.client.provider.salesforce.token-uri         | SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_SALESFORCE_TOKENURI         | string        | Salesforce Token API URL                                                                                          |
| spring.security.oauth2.client.registration.salesforce.client-id     | SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_SALESFORCE_CLIENTID     | string        | Salesforce Client ID                                                                                              |
| spring.security.oauth2.client.registration.salesforce.client-secret | SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_SALESFORCE_CLIENTSECRET | string        | Salesforce Client Secret                                                                                          |
| ccs.esourcing.salesforce.oauth2.username                            | CCS_ESOURCING_SALESFORCE_OAUTH2_USERNAME                           | string        | Salesforce username                                                                                               |
| ccs.esourcing.salesforce.oauth2.password                            | CCS_ESOURCING_SALESFORCE_OAUTH2_PASSWORD                           | string        | Salesforce password                                                                                               | 
| spring.security.oauth2.client.provider.salesforce.token-uri         | SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_SALESFORCE_TOKENURI         | string        | Salesforce Token API URL                                                                                          |

As the code is still in development the following properties are subject to change but should be set

| Property                                                     | Environment variable                                      | Type   | Description                                                                    |
| ------------------------------------------------------------ | --------------------------------------------------------- | ------ | ------------------------------------------------------------------------------ |
| ccs.esourcing.jaggaer.default.buyer-company-id               | CCS_ESOURCING_JAGGAER_DEFAULT_BUYERCOMPANYID              | string |                                                                                |
| ccs.esourcing.jaggaer.default.project-type                   | CCS_ESOURCING_JAGGAER_DEFAULT_PROJECTTYPE                 | string |                                                                                |
| ccs.esourcing.jaggaer.default.source-template-reference-code | CCS_ESOURCING_JAGGAER_DEFAULT_SOURCETEMPLATEREFERENCECODE | string |                                                                                |
| ccs.esourcing.jaggaer.default.open-market-template-id        | CCS_ESOURCING_JAGGAER_DEFAULT_OPENMARKETTEMPLATEID        | string |                                                                                |
| ccs.esourcing.jaggaer.default.sta-template-id                | CCS_ESOURCING_JAGGAER_DEFAULT_STATEMPLATEID               | string |                                                                                |
| ccs.esourcing.salesforce.oauth2.token.expires-in             | CCS_ESOURCING_SALESFORCE_OAUTH2_TOKEN_EXPIRESIN           | string | Expires in attribute added to Salesforce Oauth2 token response to force expiry |

### Testing

A Postman collection of integration tests is included under the dev folder along with some associated environment variables.  TO use them import the collection and environment files into a postman workspace and then update the 'api-key' environment variable.  The GPaaS environment is controlled by the 'env' environment variable.  The remaining variables are used to maintain reference codes between requests.

## Licence

The project is licensed with the [MIT License](https://opensource.org/licenses/MIT).

