#!/usr/bin/env bash

keytool -genkeypair -alias localhost -keyalg RSA -keysize 4096 -storetype PKCS12 -keystore keystore.p12 -validity 365
