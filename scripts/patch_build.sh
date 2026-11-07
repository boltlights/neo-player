#!/bin/bash
sed -i 's/compileSdk { version = release(36) { minorApiLevel = 1 } }/compileSdk = 35/g' app/build.gradle.kts
sed -i 's/targetSdk = 36/targetSdk = 35/g' app/build.gradle.kts
sed -i 's/isMinifyEnabled = false/isMinifyEnabled = true/g' app/build.gradle.kts
sed -i '/testOptions/i \  ksp {\n    arg("room.schemaLocation", "$projectDir/schemas")\n  }' app/build.gradle.kts
