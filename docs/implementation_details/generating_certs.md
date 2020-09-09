# Generating certificates and store them im java keystore

## keytool
For JAVA applications there is a special repository for storing security certification  - the java key store.
If you have no keystore, you need to setup one. As you cannot create an empty keystore, we will generate an initial keypair, that we delete afterwards.  
`keytool -genkey -keyalg="RSA" -alias` *some-server-name* `-dname "CN=TEMP" -keystore` *filename-of-your-keystore* `-storepass` *password-to-access-keystore* `-deststoretype pkcs12 -noprompt`  

`keytool` -delete -alias *some-server-name* -keystore *filename-of-your-keystore* -storepass *password-to-access-keystore*

You end up with an empty java keystore at *filename-of-your-keystore*.  


Assuming you have a PKCS12 certificate bundle, to insert this to the JKS you need to do the following:

`keytool -importkeystore -srckeystore` *your-pkcs12file.p12* `-srcstorepass` *your-pkcs12-password-for-that-file* `-sourcestoretype pkcs12 -deststore` *filename-of-your-keystore* `-deststoretype jks -deststorepass` *password-to-access-keystore*

