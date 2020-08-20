# Deployment Guide European Federation Gateway Service
by Alexander Stiefel (alexander.stiefel@t-systems.com)

##	Introduction
This document is intended to provide all needed information to deploy the European Federation Gateway Service (EFGS). Target audience are software engineers.

This document is in draft status, expect major aspects to be changed.

## Overview

# Deployment Guide

## Common Aspects

### DB Setup
The DB setup is done automatically at the first start of the application using liquibase. Due to the configration needed to be executed a high priviledged (admin) user is needed.

## Local Isolated Deployment

## Deployment Test Environment

Preconditions
- EFGS Software artefact, aka "WAR File"
- Tomcat 9 installed
- Java 11 installed
- (if desired) mySQL DB server with
  - a admin user, privilidges needed:
    - tbd.
  - an empty schema created
  - a JDBC connection string for the schema

- (if needed) proxy for outward communication
- certificates for TLS
- registered domain (aka DNS entry)
- TLS server certificate
- List of client certificates owned by the countries



### Initial Configuration

1. Upload TLS server certificates to load balancer F5 
1. Confifure load balancer to accept client certificates
1. Configuration Reverse Proxy Farm (Blue Coat) to accept the request and pass them


## Environment Specific Configuration


| property  | OS property name |   Content                                          | Example Value                          |
| --------- | --------- | ------------------------------------------------ | -------------------------------------- |
| spring.datasource.url | SPRING_DATASOURCE_URL | The jdbc connection string for the mySQL DB | jdbc:mysql://localhost:3306/fg |
| spring.datasource.username     | SPRING_DATASOURCE_USERNAME  | sa |
| spring.datasource.password  | SPRING_DATASOURCE_PASSWORD | sa |
| spring.datasource.driver-class-name | SPRING_DATASOURCE_DRIVER-CLASS-NAME | **legacy propery is fixed for all environments, will be removed from the list in the next release** | com.mysql.cj.jdbc.Driver                                  |
| spring.jpa.database-platform    | SPRING_JPA_DATABASE-PLATFORM |  **legacy propery is fixed for all environments, will be removed from the list in the next release** | org.hibernate.dialect.MySQL5InnoDBDialect                       |
| efgs.callback.proxy-host      | EFGS_CALLBACK_PROXY-HOST |proxy host name  | localhost |
| efgs.callback.proxy-port   | EFGS_CALLBACK_PROXY-PORT | proxy host port  | 1234 |


# Smoke Testing

# Test data provisioning 

# Open Issues
- add proxy authentification
- provide specific list of mysql roles the db user has to part
- do we second db user
- test national country
-- signing certificates
- how to handle env specific configuration
