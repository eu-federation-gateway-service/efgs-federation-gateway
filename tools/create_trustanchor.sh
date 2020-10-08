#!/bin/bash

keystorefilename="efgs-ta.jks"
keystorepassword="3fgs-p4ssw0rd"
certCN="EFGS-TrustAnchor DEV"
certC="DE"

echo [1 of 5] Deleting old files...
rm trustanchor.pem
rm trustanchor.pem
rm ${keystorefilename}
echo ... old files deleted.

read -p "keystorefilename [${keystorefilename}]:" input
keystorefilename=${input:-${keystorefilename}}
read -p "keystorepassword [${keystorepassword}]: " input
keystorepassword=${input:-${keystorepassword}}
read -p "certCN [${certCN}]: " input
certCN=${input:-${certCN}}
read -p "certC [${certC}]: " input
certC=${input:-${certC}}

echo [2 of 5] Creating certificate...
openssl req -nodes -new -x509 -keyout trustanchor.key -out trustanchor.pem -days 720 -subj "//C=${certC}\CN=${certCN}\O=TrustAnchor Dev Org"
echo ... Certificate created.

echo [3 of 5] Creating keystore...
keytool -genkey -keyalg RSA -keystore ${keystorefilename} -storepass ${keystorepassword} -keysize 2048 -alias dummykey -dname C=X -validity 1 -keypass ${keystorepassword}
echo ... keystore created.

echo [4 of 5] Adding certificate to keystore...
keytool -importcert -alias efgs_trust_anchor -file trustanchor.pem -keystore ${keystorefilename} -storepass ${keystorepassword} -noprompt
echo ... certificate added.

echo [5 of 5] Deleting dummy cert from keystore...
keytool -delete -keystore ${keystorefilename} -storepass ${keystorepassword} -alias dummykey
echo ... deleted dummy cert from keystore.
