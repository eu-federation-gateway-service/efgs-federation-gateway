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
![ci](https://github.com/eu-federation-gateway-service/efgs-federation-gateway/workflows/ci/badge.svg)
[![quality gate](https://sonarcloud.io/api/project_badges/measure?project=eu-federation-gateway-service_efgs-federation-gateway&metric=alert_status)](https://sonarcloud.io/dashboard?id=eu-federation-gateway-service_efgs-federation-gateway)

### Prerequisites
 - [Open JDK 11](https://openjdk.java.net)  
 - [Maven](https://maven.apache.org)

### Build
Whether you cloned or downloaded the 'zipped' sources you will either find the sources in the chosen checkout-directory or get a zip file with the source code, which you can expand to a folder of your choice.

In either case open a terminal pointing to the directory you put the sources in. The local build process is described afterwards depending on the way you choose.

#### Maven based build
This is the recommended way for taking part in the development.  
Please check, whether following prerequisites are installed on your machine:
- [Open JDK 11](https://openjdk.java.net) or a similar JDK 11 compatible VM  
- [Maven](https://maven.apache.org)

#### API documentation  
Along with the application there comes a [swagger2](https://swagger.io) API documentation, which you can access in your web browser when the efgs-gateway-service applications runs:

    <base-url>/swagger-ui/index.html

Which results in the following URL on your local machine:
http://localhost:8090/swagger-ui/index.html

## Documentation  

Additional documentation can be found in the folder [docs](./docs/).

## Support and feedback
The following channels are available for discussions, feedback, and support requests:

| Type                     | Channel                                                |
| ------------------------ | ------------------------------------------------------ |
| **Federation gateway issues**    | <a href="https://github.com/eu-federation-gateway-service/efgs-federation-gateway/issues" title="Open Issues"><img src="https://img.shields.io/github/issues/eu-federation-gateway-service/efgs-federation-gateway?style=flat"></a>  |
| **Other requests**    | <a href="mailto:efgs-opensource@telekom.de" title="Email EFGS Team"><img src="https://img.shields.io/badge/email-EFGS%20team-green?logo=mail.ru&style=flat-square&logoColor=white"></a>   |

## How to contribute  
Contribution and feedback is encouraged and always welcome. For more information about how to contribute, the project structure, as well as additional contribution information, see our [Contribution Guidelines](./CONTRIBUTING.md). By participating in this project, you agree to abide by its [Code of Conduct](./CODE_OF_CONDUCT.md) at all times.

## Contributors  
Our commitment to open source means that we are enabling -in fact encouraging- all interested parties to contribute and become part of its developer community.

## Licensing
Copyright (C) 2020 T-Systems International GmbH and all other contributors

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [LICENSE](./LICENSE) for the specific language governing permissions and limitations under the License.
