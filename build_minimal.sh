#!/bin/bash
echo "Building PixelXpert Minimal APK..."
chmod +x gradlew
./gradlew :app:assembleRelease

echo "Packaging KernelSU/Magisk Module..."
mkdir -p MagiskModBase/system/priv-app/PixelXpert
cp app/build/outputs/apk/release/PixelXpert.apk MagiskModBase/system/priv-app/PixelXpert/

cd MagiskModBase
zip -r -9 -q ../PixelXpert-Minimal.zip *
cd ..

rm -rf MagiskModBase/system/priv-app/PixelXpert/PixelXpert.apk
echo "Done! File generated: PixelXpert-Minimal.zip"
