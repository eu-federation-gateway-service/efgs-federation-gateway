# Deployment Guide European Federation Gateway Service
by Alexander Stiefel (alexander.stiefel@t-systems.com)

##	Introduction
This document is intended to provide all needed information to deploy the European Federation Gateway Service (EFGS). Target audience are software engineers.

This document is in draft status, expect major aspects to be changed.

## Overview

# Deployment Guide

## Common Aspects

environment specific configuration is stored in a property file

### DB Setup
The DB tables will be created  automatically at the first start of the application using liquibase. All structure changes
and migrations will be done by Liquibase.

Due to the configuration needed for the MySQL 5.6 it is necessary to execute some Statements at a high privileged (SUPER) user level.
Therefore, the DB-user used by the application needs Super rights.
The empty schema used by the Application needs to be created before starting the application.
## Local Isolated Deployment

## Deployment Test Environment
Preconditions
- EFGS Software artefact, aka "WAR File"

- Tomcat 9 installed
  - DB connection is managed by tomcat and accessed via JNDI
    - name of the connection resource is "jdbc/efgs"

- Java 11 installed
- (if desired) mySQL DB server with
  - a admin user, privileges needed:
    - SUPER.
  - an empty schema named 'efgs' created
- (if needed) proxy for outward communication
- certificates for TLS
- registered domain (aka DNS entry)
- TLS server certificate
- List of client certificates owned by the countries



### Initial Configuration

1. Upload TLS server certificates to load balancer F5 
1. Configure load balancer to accept client certificates
1. Configuration reverse Proxy Farm (Blue Coat) to accept the request and pass them


## Environment Specific Configuration
| property  | OS property name |   Content                                          | Example Value                          |
| --------- | --------- | ------------------------------------------------ | -------------------------------------- |
| efgs.callback.proxy-host      | EFGS_CALLBACK_PROXY-HOST |proxy host name  | localhost |
| efgs.callback.proxy-port   | EFGS_CALLBACK_PROXY-PORT | proxy host port  | 1234 |


# Smoke Testing

# Test data provisioning 

# Open Issues
- add proxy authentication
- do we second db user 
    will be needed in future not for Test
- test national country
-- signing certificates
- how to handle env specific configuration
    -will be handled by DIGIT zip format 
    -need script to create these
