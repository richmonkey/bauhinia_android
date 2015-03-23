#!/bin/sh
keytool -genkey -v -keystore gradle.keystore -alias gradle -keyalg RSA -keysize 2048 -validity 10000
