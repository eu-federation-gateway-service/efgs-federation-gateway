@ECHO OFF

SET /P "keystorefilename=KeyStore Filenname [efgs-cb-client.jks]: " || SET "keystorefilename=efgs-cb-client.jks"
SET /P "keystorepassword=KeyStore Password [3fgs-p4ssw0rd]: " || SET "keystorepassword=3fgs-p4ssw0rd"
SET /P "certCN=Certificate CommonName [EFGS-Callback DEV]: " || SET "certCN=EFGS-Callback DEV"
SET /P "certC=Certificate Country [DE]: " || SET "certC=DE"

ECHO [1 of 6] Deleting old files...
DEL callback.pem
DEL callback.key
DEL callback.p12
DEL %keystorefilename%
ECHO ... old files deleted.

ECHO [2 of 6] Creating certificate...
openssl req -nodes -new -x509 -keyout callback.key -out callback.pem -days 720 -subj "/C=%certC%/CN=%certCN%/O=EFGS DEV Org"
ECHO ... Certificate created.

ECHO [3 of 6] Creating keystore...
keytool -genkey -keyalg RSA -keystore %keystorefilename% -storepass %keystorepassword% -keysize 2048 -alias dummykey -dname C=X -validity 1 -keypass %keystorepassword%
ECHO ... keystore created.

ECHO [4 of 6] Creating combined certificate...
openssl pkcs12 -export -in callback.pem -inkey callback.key -out callback.p12 -password pass:%keystorepassword% -name efgs_callback_key
ECHO ... combined certificate created.

ECHO [5 of 6] Adding certificates to keystore...
keytool -importkeystore -deststorepass %keystorepassword% -destkeypass %keystorepassword% -destkeystore %keystorefilename% -srckeystore callback.p12 -srcstoretype PKCS12 -srcstorepass %keystorepassword% -alias efgs_callback_key -noprompt
keytool -importcert -alias efgs_callback_cert -file callback.pem -keystore %keystorefilename% -storepass %keystorepassword% -noprompt
ECHO ... certificates added.

ECHO [6 of 6] Deleting dummy cert from keystore...
keytool -delete -keystore %keystorefilename% -storepass %keystorepassword% -alias dummykey
ECHO ... deleted dummy cert from keystore.
