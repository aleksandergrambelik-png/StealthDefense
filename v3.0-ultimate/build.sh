#!/bin/bash
echo "[+] Compiling SanderUltimate v3.0..."
javac -cp "lib/SanderCoreLib.jar" -d build $(find src -name "*.java")
if [ $? -ne 0 ]; then echo "[!] Compile Failed!"; exit 1; fi

cd build
jar xf ../lib/SanderCoreLib.jar
cd ..
jar cvfe SanderUltimate.jar com.sander.Main -C build . > /dev/null
echo "[+] BUILD SUCCESSFUL!"
