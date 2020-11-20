#!/bin/bash

# fail on error
set -e

certFileName="client.pem"
signCertFileName="trustanchor.key"
signature=""
read -p "Input file containing the cert to sign [${certFileName}]:" input
certFileName=${input:-${certFileName}}
read -p "Input file containing the signing Certificate [${signCertFileName}]: " input
signCertFileName=${input:-${signCertFileName}}
currentdate=$(date -u +"%Y-%m-%d %H:%M:%S")
echo [1 of 4] Checking input file...

echo ... input file ok.

echo [2 of 4] Calculating Signature...
openssl dgst -sha256 -sign ${signCertFileName} -out sig.tmp ${certFileName}
echo ... signature calculated.

echo [3 of 4] Saving calculated signature...
openssl base64 -in sig.tmp -out signature.base64 -A
echo ... saved to signature.base64
signature=$(cat signature.base64)

cert_base64=$(cat ${certFileName} | base64)
#echo $cert_base64

openssl x509 -fingerprint -sha256 -in ${certFileName} -noout > fingerprint.sha256
fingerprint=$(cat fingerprint.sha256)
echo ${fingerprint}
fingerprint=${fingerprint:19}
echo ${fingerprint}
fingerprint=${fingerprint//:/""}
echo ${fingerprint}
fingerprint=$(echo "$fingerprint" | tr '[:upper:]' '[:lower:]')
echo ${fingerprint}

DN=$(openssl x509 -in $certFileName -noout -subject)
#extract the country from DN
country=${DN:12:2}

read -p "Is it for AUTHENTICATION or SIGNING [A|S]:" input
if [ $input = "A" ];then
purpose='AUTHENTICATION'
elif [ $input = "S" ];then
purpose='SIGNING'
fi


template="INSERT INTO certificate VALUES(NULL, '$currentdate', '$fingerprint', '$country', '$purpose', FALSE, NULL, '$signature', FROM_BASE64('$cert_base64'))";
echo $template > insert.sql

echo [4 of 4] Cleaning up...
rm sig.tmp
rm fingerprint.sha256
echo ... cleaned up.
