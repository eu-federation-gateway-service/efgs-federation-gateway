@ECHO OFF

SET /P "certFileName=Input Certificate to sign filename [client.pem]: " || SET "certFileName=client.pem"

ECHO ##########################################################
ECHO ##########################################################
ECHO ###                  IMPORTANT NOTICE:                 ###
ECHO ###       PLEASE CHECK THAT CERTIFICATE DOES NOT       ###
ECHO ### CONTAIN PRIVATE KEY AND HAS UNIX LINE ENDINGS (\n) ###
ECHO ##########################################################
ECHO ##########################################################

SET /P "signCertFileName=Signing Certificate filename (TrustAnchor) [trustanchor.key]: " || SET "signCertFileName=trustanchor.key"

ECHO [1 of 5] Checking input file...

REM TODO: Automatic check for line endings.

ECHO ... input file ok.

ECHO [2 of 5] Calculating Signature...
openssl dgst -sha256 -sign %signCertFileName% -out sig.tmp %certFileName%
ECHO ... signature calculated.

ECHO [3 of 5] Saving calculated signature...
openssl base64 -in sig.tmp -out signature.base64 -A
ECHO ... saved to signature.base64

ECHO [4 of 5] Generating INSERT statement...

SET /P SIGNATURE=<signature.base64
SET ISODATE=%date:~6,4%-%date:~3,2%-%date:~0,2%T%time:~0,2%:%time:~3,2%:%time:~6,2%
SET ISODATE=%ISODATE: =0%
SET ISODATE=%ISODATE:T= %

setlocal EnableDelayedExpansion

SET RAW=
FOR /f "tokens=*" %%i IN ('type %certFileName%') DO (
    SET RAW=!RAW!%%i\n
)

openssl x509 -fingerprint -sha256 -in %certFileName% -noout > HASH.txt
SET /P HASH=<HASH.txt
SET HASH=%HASH::=%
SET HASH=%HASH:~-64%

SET "_UCASE=ABCDEF"
SET "_LCASE=abcdef"

FOR /l %%a IN (0,1,5) DO (
   CALL SET "_FROM=%%_UCASE:~%%a,1%%
   CALL SET "_TO=%%_LCASE:~%%a,1%%
   CALL SET "HASH=%%HASH:!_FROM!=!_TO!%%
)

openssl x509 -in %certFileName% -noout -subject
SET /P "COUNTRY=Please enter value for C Attribute from output above [DE]: " || SET "COUNTRY=DE"
SET /P "TYPEQ=Is this cert an AUTHENTICATION cert? [yes]: " || SET "TYPEQ=yes"

IF %TYPEQ%==yes (
    SET TYPE=AUTHENTICATION
) ELSE (
    SET TYPE=SIGNING
)


SET template=INSERT INTO certificate VALUES(NULL, '%ISODATE%', '%HASH%', '%COUNTRY%', '%TYPE%', FALSE, NULL, '%SIGNATURE%', '%RAW%');
ECHO %template% > insert.sql

ECHO [4 of 5] Insert statement created.

ECHO [5 of 5] Cleaning up...
DEL sig.tmp
DEL HASH.txt
ECHO ... cleaned up.
