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
- (if desired) mySQL DB server with a admin user
- (if needed) proxy for outward communication
- certificates for TLS
- registered domain (aka DNS entry)



### Initial Configuration


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

