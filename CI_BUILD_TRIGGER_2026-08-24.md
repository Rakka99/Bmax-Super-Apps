# CI build trigger

This file intentionally triggers the Android CI workflow after the CI keystore fix. The workflow must generate the debug keystore directly on the runner, build `assembleDebug`, verify the APK, and upload `bmax-super-apps-debug-apk`.
