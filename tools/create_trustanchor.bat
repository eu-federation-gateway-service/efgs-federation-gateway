@ECHO OFF

SET /P "keystorefilename=KeyStore Filenname [efgs-ta.jks]: " || SET "keystorefilename=efgs-ta.jks"
SET /P "keystorepassword=KeyStore Password [3fgs-p4ssw0rd]: " || SET "keystorepassword=3fgs-p4ssw0rd"
SET /P "certCN=Certificate CommonName [EFGS-TrustAnchor DEV]: " || SET "certCN=EFGS-TrustAnchor DEV"
SET /P "certC=Certificate Country [DE]: " || SET "certC=DE"

ECHO [1 of 5] Deleting old files...
DEL trustanchor.pem
DEL trustanchor.key
DEL %keystorefilename%
ECHO ... old files deleted.

ECHO [2 of 5] Creating certificate...
openssl req -nodes -new -x509 -keyout trustanchor.key -out trustanchor.pem -days 720 -subj "/C=%certC%/CN=%certCN%/O=TrustAnchor Dev Org"
ECHO ... Certificate created.

ECHO [3 of 5] Creating keystore...
keytool -genkey -keyalg RSA -keystore %keystorefilename% -storepass %keystorepassword% -keysize 2048 -alias dummykey -dname C=X -validity 1 -keypass %keystorepassword%
ECHO ... keystore created.

ECHO [4 of 5] Adding certificate to keystore...
keytool -importcert -alias efgs_trust_anchor -file trustanchor.pem -keystore %keystorefilename% -storepass %keystorepassword% -noprompt
ECHO ... certificate added.

ECHO [5 of 5] Deleting dummy cert from keystore...
keytool -delete -keystore %keystorefilename% -storepass %keystorepassword% -alias dummykey
ECHO ... deleted dummy cert from keystore.
