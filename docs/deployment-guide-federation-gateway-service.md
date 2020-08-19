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

## Environment Specific Configuration


| property  | Content                                          | Example Value                          |
| --------- | ------------------------------------------------ | -------------------------------------- |
| spring.datasource.url | The jdbc connection string for the mySQL DB | jdbc:mysql://localhost:3306/fg |
| spring.datasource.username     | db user name  | sa |
| spring.datasource.password  | db user password | sa |
| spring.datasource.driver-class-name       | **legacy propery is fixed for all environments, will be removed from the list in the next release** | com.mysql.cj.jdbc.Driver                                  |
| spring.jpa.database-platform     | **legacy propery is fixed for all environments, will be removed from the list in the next release** | org.hibernate.dialect.MySQL5InnoDBDialect                       |
| efgs.callback.proxy-host      | proxy host name  | localhost |
| efgs.callback.proxy-port   | proxy host port  | 1234 |


# Smoke Testing

# Test data provisioning

