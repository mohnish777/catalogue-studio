# Catalogue Studio

An Android product catalogue built with Kotlin and Jetpack Compose. The planned product discovery, search and email-enquiry flows will use a local Node.js backend.

## Current checkpoint

The Android project setup and visual foundation are ready. The app launches a minimal screen displaying "Catalogue Studio". The backend currently provides only a `/health` endpoint. Product features, email delivery and Android-to-backend requests are not implemented yet; no backend is needed to run the placeholder screen.

## Run in Android Studio

1. Open the repository's `android/` directory as the project.
2. Use JDK 17 for Gradle and install Android SDK Platform 36 through SDK Manager.
3. Let Gradle sync. The wrapper selects Gradle 8.14.3 automatically.
4. Select the `app` run configuration and an emulator or device running API 26 or newer, then press Run.

The project pins the Gradle build process to a locally installed JDK 17 through `android/gradle/gradle-daemon-jvm.properties`. This is separate from the Java/Kotlin bytecode targets in the app build script. Use a full JDK containing `bin/jlink`; a runtime-only Java installation can fail Android's `JdkImageTransform` even when Java itself starts successfully.

Android Studio records your SDK location in `android/local.properties`, which is ignored by Git. For command-line builds, either set `ANDROID_HOME` to your SDK directory or create that file with `sdk.dir=/absolute/path/to/Android/sdk`.

## Build from the command line

From the repository root:

```sh
cd android
./gradlew assembleDebug --console=plain
```

The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`, relative to the repository root.

To install and launch on a running emulator, from `android/`:

```sh
adb devices
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.cataloguestudio/.MainActivity
```

Replace `emulator-5554` with the intended device serial from `adb devices`. The CLI commands assume `adb` is on your PATH.

Run Android static checks with:

```sh
./gradlew lintDebug --console=plain
```

## Run the local backend

Use Node.js 22 or newer. With nvm installed, select the project's version from the repository root:

```sh
nvm install
nvm use
cd backend
npm ci
npm run dev
```

The server listens on `127.0.0.1:3000`. Open [the health endpoint](http://127.0.0.1:3000/health), or run:

```sh
curl http://127.0.0.1:3000/health
```

Expected response: `{"status":"ok"}`. Stop the server with Ctrl+C. `npm run dev` watches the TypeScript files and restarts when they change; type checking is a separate command.

From `backend/`, verify and run the compiled JavaScript with:

```sh
npm run typecheck
npm run build
npm start
```

Run either the development server or the compiled server at a time. If port 3000 is already occupied, use `PORT=3001 npm run dev` and request port 3001 instead.

The server currently binds to your computer's loopback interface for local use. Mailpit, Docker and product routes belong to later chunks.

## Android backend address for later slices

The generated `BuildConfig.BASE_URL` defaults to `http://10.0.2.2:3000`. Inside the Android Emulator, `10.0.2.2` reaches your computer's loopback interface. This address is configured now; no HTTP client or network request exists yet.

Override it at build time when needed:

```sh
./gradlew assembleDebug -PapiBaseUrl=https://your-backend.example
```

For a physical device using a future local backend, use your computer's reachable LAN address instead of `10.0.2.2`. Plain HTTP is permitted only by the debug manifest. A release build needs an HTTPS backend address.

## Project structure

```text
android/
├── settings.gradle.kts       Project repositories and :app registration
├── build.gradle.kts          Root plugin declarations
├── gradle/libs.versions.toml Plugin and library versions
└── app/
    ├── build.gradle.kts      Android SDK, JVM, Compose and BASE_URL settings
    └── src/
        ├── debug/AndroidManifest.xml
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/cataloguestudio/MainActivity.kt
            └── res/values/strings.xml

backend/
├── package.json             Node dependencies and scripts
├── tsconfig.json            TypeScript compiler settings
└── src/
    ├── app.ts               Express application and HTTP routes
    └── server.ts            Port configuration and server startup
```
