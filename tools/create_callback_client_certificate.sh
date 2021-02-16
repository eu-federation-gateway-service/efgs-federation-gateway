#!/bin/bash

# fail on error
set -e

keystorefilename="efgs-cb-client.jks"
keystorepassword="3fgs-p4ssw0rd"
certCN="EFGS-Callback DEV"
certC="DE"              
yn=N

echo [1 of 6] Deleting old files...
rm -f callback.pem
rm -f callback.key
rm -f callback.p12
rm -f ${keystorefilename}
echo ... old files deleted.
read -p "keystorefilename [${keystorefilename}]:" input
keystorefilename=${input:-${keystorefilename}}
read -p "keystorepassword [${keystorepassword}]: " input
keystorepassword=${input:-${keystorepassword}}
read -p "certCN [${certCN}]: " input
certCN=${input:-${certCN}}
read -p "certC [${certC}]: " input
certC=${input:-${certC}}
#while true; do
#read -p "include public key in generated cert [${yn}]: " input
#yn=${input:-$yn}
# case $yn in
#        [Yy]* ) pk=-pubkey; break;;
#        [Nn]* ) break;;
#        * ) echo "Please answer Y(es) or N(o).";;
# esac
#done

case "$(uname -s)" in
  MINGW32*|MSYS*|MINGW*)
    # Work around MinGW/MSYS's path conversion (http://www.mingw.org/wiki/Posix_path_conversion) 
    certSubject="//C=${certC}\CN=${certCN}\O=EFGS DEV Org"
  ;;
  *)
    certSubject="/C=${certC}/CN=${certCN}/O=EFGS DEV Org"
  ;;
esac

echo [2 of 6] Creating certificate...
openssl req -nodes -new -x509 -keyout callback.key -out callback.pem -days 720 -subj "$certSubject"
echo ... Certificate created.

echo [3 of 6] Creating keystore...
keytool -genkey -keyalg RSA -keystore ${keystorefilename} -storepass ${keystorepassword} -keysize 2048 -alias dummykey -dname C=Country -validity 1 -keypass ${keystorepassword}
echo ... keystore created.

echo [4 of 6] Combine certificate and private key, output to PKCS#12 file...
openssl pkcs12 -export -in callback.pem -inkey callback.key -out callback.p12 -password pass:${keystorepassword} -name efgs_callback_key
echo ... combined certificate created.

echo [5 of 6] Adding certificates to keystore...
keytool -importkeystore -deststorepass ${keystorepassword} -destkeypass ${keystorepassword} -destkeystore ${keystorefilename} -srckeystore callback.p12 -srcstoretype PKCS12 -srcstorepass ${keystorepassword} -alias efgs_callback_key -noprompt
keytool -importcert -alias efgs_callback_cert -file callback.pem -keystore ${keystorefilename} -storepass ${keystorepassword} -noprompt
echo ... certificates added.

echo [6 of 6] Deleting dummy cert from keystore...
keytool -delete -keystore ${keystorefilename} -storepass ${keystorepassword} -alias dummykey
echo ... deleted dummy cert from keystore.
