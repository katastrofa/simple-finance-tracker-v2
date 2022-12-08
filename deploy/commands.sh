#!/usr/bin/env bash

keytool -genkeypair -alias localhost -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore keystore.p12 -validity 365


scp db/src/main/resources/db/* sft:~/db/
scp -r target/deployment/* sft:~/sft/
scp backend/src/main/resources/prod.conf sft:~/sft/
scp frontend/src/main/resources/index-prod.html sft:~/sft/static-assets/index-main.html

cd sft/deployment
java -Dlogback.configurationFile=logback.xml -Dconfig.file=prod.conf -jar simple-finance-tracker-v2.jar &
