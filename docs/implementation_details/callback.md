# Implementation Details Callback Feature
The callback feature notifies participating countries about newly created diagnosis key batches which are ready to download.
The following document describes details about the implementation of the callback system.

## Data Model
To manage callback subscriptions and outstanding callback requests over multiple instances of EFGS it is required to store all static and dynamic information within the database.

The CallbackSubscription entity holds all information about who wants to get notified where.

| Field | Type | Example value | Description |
| --- | --- | --- | --- |
| id | Long | 1 | Internal DB id of the entry |
| callbackId | String | de-cb-001 | Freetext identifier a country has to provide to identify its own callback |
| createdAt | ZonedDateTime | 2020-08-17 12:14:52.254 | Timestamp when the subscription was created |
| url | String | https://srv01.local.backend.de | Target URL the callback has to be sent to |
| country | String | DE | The country who has subscribed for callback |

An instance of CallbackTask entity will be created for each created diagnosis key batch and callback subscriber.
The CallbackTask hold information to safely send a callback only once over multiple instances.
 
| Field | Type | Example value | Description |
| --- | --- | --- | --- |
| id | Long | 1 | Internal DB id of the entry |
| createdAt | ZonedDateTime | 2020-08-17 12:14:52.254 | Timestamp when the callback task was created |
| executionLock | ZonedDateTime | 2020-08-17 12:16:32.123 | If this value is set, one instance currently works on this task. Timestamp will be set on begin of execution |
| lastTry | ZonedDateTime | 2020-08-17 12:17:01.001 | Timestamp when the last try has failed |
| retries | int | 2 | Amount of failed attempts to send the callback |
| notBefore | CallbackTaskEntity | _Object { ... }_ | Reference to the preceding CallbackTask to ensure the order is correct |
| batch | DiagnosisKeyBatchEntity | _Object { ... }_ | Reference to the diagnosis key batch that has to be announced |
| callbackSubscription | CallbackSubscriptionEntity | _Object { ... }_ | Reference to the callback subscription |

## Security

### URL Check

Before a URL can be used for callbacks several checks need to be done:

1.   The given URL must be parsable.
1.   A URL must use https protocol.
1.   The URL must not contain any query parameters.
1.   The hostname must be resolveable with public DNS Servers.
1.   The hostname must not resolve to a IP-Address of private ranges:
     * 10.0.0.0/8
     * 127.0.0.0/8
     * 100.64.0.0/10
     * 169.254.0.0/16
     * 172.16.0.0/12
     * 192.168.0.0/16
     * ::1
     * fc00::/7
1.   A valid certificate of type CALLBACK and country is subscribers country and host is URL's host must be present in certificate table        

## Logical Implementation

EFGS has to deal with callbacks on different stages:

### Callback Subscribing

1.   A participating country sends a PUT request to the callback admin endpoint with callbackId and target url.
1.   Search for CallbackEntities with given callbackId.
     
     **Callback Entity exisits**: Update the CallbackSubscriptionEntity if requesters country matches the subscriptions country, else reject with Not Allowed.
     
     **Callback Entity doesn't exisits**: A new instance of CallbackSubscriptionEntity has to be created. Extract the country from request properties.
     
     Before an url can be inserted as target url it has to be checked. (see url check)
     
### New Batch has been Created

1.   Query all CallbackSubscriptionEntities from database
1.   Create a CallbackTaskEntity for each subscription **s** for batch b:
     * set execution_lock = null
     * set last_try = null
     * set retries = 0
     * set callbackSubscription = s
     * set batch = b
     * query db for CallbackTasks with subscription = s order by created_At DESC
     
       **Callback Task found**: set the first found task (the one with the latemost created_at) as not_before
       
       **No Callback Task found**: set not_before = null
1.   Insert into database

### Task Executor

The task executor has to be triggered in a fixed interval (e.g. every 5 minutes).
All parameters which are defined as X have to be configurable.

1.   Search for CallbackTaskEntities which:
     * not_before is null
     * execution_lock is null
     * last_try is older than X minutes
1.   Take the first found CallbackTaskEntity **t** or exit method if none found
1.   Set execution_lock to currentTimestamp (this change must be committed immediately)
1.   Perform URL security check (see url check)
1.   Query database for certificate **c** which:
     * type is CALLBACK
     * country is t.callbackSubscription.country
     * revoked is false
     * host is host of t.callbackSubscription.url
1.   Send callback request to url
     * GET method
     * set new batch information as request params (date and batchTag)
     * set request header "X-SSL-Client-SHA256" to c.thumbprint (the reverse proxy will resolve this to its stored mTLS certificate)
1.   Evaluate request result:
     * Request not was successful: 
        * increase last_try by one if not already reached X
        * If X is reached create a log entry and execute steps from "Request was successful"
        * set execution_lock = null
        * set last_try to current timestamp
     * Request was successful:
       * Search for CallbackTaskEntity with t as not_before property
       * If found, set not_before property to null
       * Delete t from database
1. Continue with 1.

### Execution Lock Cleanup

If an instance of EFGS crashes or is stopped during callback execution a CallbackTaskEntity with set execution lock would be abandoned in database and never executed.
To avoid this it is required to automatically remove execution locks after a specific time.
All parameters which are defined as X have to be configurable.

1.   Search for CallbackTaskEntities which:
     * execution_lock is older than X minutes
1.   Set execution_lock to null

### Callback Unsubscribing

If a backend doesn't want to get notified any more about new batches it has to unsubscribe via a DELETE request to the callback admin endpoint.
This is where the national backend needs its callbackId that it has given at subscribing time.

EFGS tasks when a country unsubscribes:
1.   Query database for CallbackSubscriptionEntity which:
     * callbackId is given callbackId
     * country is country extracted from request properties
1.   For each CallBackSubscriptionEntity c
     * Delete all CallbackTaskEntities from database where callbackSubscription is c 
1.   If found, delete.  

