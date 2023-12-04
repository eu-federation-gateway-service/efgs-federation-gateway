<h1 align="center">
    EU Federation Gateway Service
</h1>
 
<p align="center">
    <a href="https://github.com/eu-federation-gateway-service/efgs-federation-gateway/commits/" title="Last Commit"><img src="https://img.shields.io/github/last-commit/eu-federation-gateway-service/efgs-federation-gateway?style=flat"></a>
    <a href="https://github.com/eu-federation-gateway-service/efgs-federation-gateway/issues" title="Open Issues"><img src="https://img.shields.io/github/issues/eu-federation-gateway-service/efgs-federation-gateway?style=flat"></a>
    <a href="https://github.com/eu-federation-gateway-service/efgs-federation-gateway/blob/master/LICENSE" title="License"><img src="https://img.shields.io/badge/License-Apache%202.0-green.svg?style=flat"></a>
</p>

<p align="center">
  <a href="#development">Development</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#support-and-feedback">Support</a> •
  <a href="#how-to-contribute">Contribute</a> •
  <a href="#contributors">Contributors</a> •
  <a href="#licensing">Licensing</a>
</p>

The goal of this project is to develop the official European solution for the interoperability between national backend servers of decentralised contact tracing applications to combat COVID-19.

This repository contains the **federation gateway service**.

## Status
![ci](https://github.com/eu-federation-gateway-service/efgs-federation-gateway/workflows/ci-master/badge.svg)
[![quality gate](https://sonarcloud.io/api/project_badges/measure?project=eu-federation-gateway-service_efgs-federation-gateway&metric=alert_status)](https://sonarcloud.io/dashboard?id=eu-federation-gateway-service_efgs-federation-gateway)
[![coverage](https://sonarcloud.io/api/project_badges/measure?project=eu-federation-gateway-service_efgs-federation-gateway&metric=coverage)](https://sonarcloud.io/dashboard?id=eu-federation-gateway-service_efgs-federation-gateway)
[![bugs](https://sonarcloud.io/api/project_badges/measure?project=eu-federation-gateway-service_efgs-federation-gateway&metric=bugs)](https://sonarcloud.io/dashboard?id=eu-federation-gateway-service_efgs-federation-gateway)  

SonarCloud supports the EU Federation Gateway Service project! SonarCloud (www.sonarcloud.io) catches Bugs and Vulnerabilities in your repositories, and provides clear resolution guidance for any Code Quality or Security issue it detects. SonarCloud makes applications maintainable, reliable and safe!

### Prerequisites
 - [Open JDK 11](https://openjdk.java.net) (with installed ```keytool``` CLI)
 - [Maven](https://maven.apache.org)
 - [OpenSSL](https://www.openssl.org) (with installed CLI)

### Build
Whether you cloned or downloaded the 'zipped' sources you will either find the sources in the chosen checkout-directory or get a zip file with the source code, which you can expand to a folder of your choice.

In either case open a terminal pointing to the directory you put the sources in. The local build process is described afterwards depending on the way you choose.

#### Maven based build
This is the recommended way for taking part in the development.  
Please check, whether following prerequisites are installed on your machine:
- [Open JDK 11](https://openjdk.java.net) or a similar JDK 11 compatible VM  
- [Maven](https://maven.apache.org)

#### Build Docker Image
This project also supports building a Docker image for local testing (Docker image should not be used for production environments).

In order to run EFGS locally you need four certificates:

* National Backend Personal Authentication certificate (NB[TLS]).
* National Backend Signing Certificate (NB[BS]).
* EFGS TrustAncor Certificate (EFGS[TA]).
* EFGS Outbound Callback Certificate (EFGS[TLS]).

You need to provide your own National Backend certificates. Details can be found in the certificate governance document.

The EFGS TrustAncor and Outbound Callback certificates can be created by executing the `create_trustanchor` and `create_callback_client_certificate` script from `tools` directory. These scripts will generate certifcates and pack them into a Java Key Store.

For testing purpose it is advised to not change the default parameters and answer all question of the script the the default value (just press enter) 
As a result you will have two files: `efgs-ta.jks` and `efgs-cb-client.jks`. We need these keystores later.

To build the Docker image enable the maven profile ```docker``` and build the project:

```shell script
mvn clean install -P docker
```

A directory ```docker``` will be created in ```target``` directory. Create a directory ```certs``` within this directory and put the previously created keystores inside.
Now open a shell with working directory within the created directory and execute

```shell script
docker-compose up --build
```

The EFGS Docker image will be built. Also a MySQL database will be created. After that both start up and EFGS service is available on localhost port 8080.

To access your local EFGS instance you need to whitelist your personal authentication certificate.
To do this execute the `create_certificate_signature` script from `tools` directory. The script will ask for your certificate (defaults to `client.pem`). Provide the full path to your certificate here. Please pay attention that your certificate does not contain a private key and has unix line endings. Otherwise the generated signature would not match.
As an result the script will generate a ```insert.sql``` file. This file contains one prepared SQL insert statement. Execute this statement with a MySQL Client of your choice inside your Docker MySql database.

To also be able to upload keys repeat this step with your signing certificate.

#### API documentation  
Along with the application there comes a [swagger2](https://swagger.io) API documentation, which you can access in your web browser when the efgs-gateway-service applications runs:

    <base-url>/swagger

Which results in the following URL on your local machine:
http://localhost:8080/swagger

## Documentation  

Additional documentation can be found in the folder [docs](./docs/).

## Support and feedback
The following channels are available for discussions, feedback, and support requests:

| Type                     | Channel                                                |
| ------------------------ | ------------------------------------------------------ |
| **Federation gateway issues**    | <a href="https://github.com/eu-federation-gateway-service/efgs-federation-gateway/issues" title="Open Issues"><img src="https://img.shields.io/github/issues/eu-federation-gateway-service/efgs-federation-gateway?style=flat"></a>  |
| **Other requests**    | <a href="mailto:opensource@telekom.de" title="Email EFGS Team"><img src="https://img.shields.io/badge/email-EFGS%20team-green?logo=mail.ru&style=flat-square&logoColor=white"></a>   |

## How to contribute  
Contribution and feedback is encouraged and always welcome. For more information about how to contribute, the project structure, as well as additional contribution information, see our [Contribution Guidelines](./CONTRIBUTING.md). By participating in this project, you agree to abide by its [Code of Conduct](./CODE_OF_CONDUCT.md) at all times.

## Contributors  

  - [CISPA Helmholtz Center for Information Security](https://www.cispa.de/) (Contact: [Cas Cremers](https://cispa.saarland/group/cremers/index.html))

Our commitment to open source means that we are enabling -in fact encouraging- all interested parties to contribute and become part of its developer community.

## Licensing
Copyright (C) 2020 T-Systems International GmbH and all other contributors

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.
