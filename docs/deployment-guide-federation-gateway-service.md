# Deployment Guide Federation Gateway Service
by Alexander Stiefel (alexander.stiefel@t-systems.com)

##	Introduction

## Overview

# Deployment Guide

## Local Isolated Deployment

## Deployment Test Environment

Preconditions
- EFGS Software artefact, aka "WAR File"
- Tomcat 9 installed
- Java 11 installed

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
