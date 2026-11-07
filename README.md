# Catalogue Studio

An Android product catalogue built with Kotlin and Jetpack Compose. The planned product discovery, search and email-enquiry flows will use a local Node.js backend.

## Current checkpoint

The Android project setup and visual foundation are ready. The app launches a minimal screen displaying "Catalogue Studio". The backend provides only a `/health` endpoint, and Mailpit supplies a local test inbox. Product features, enquiry sending and Android-to-backend requests are not implemented yet; no backend is needed to run the placeholder screen.

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

## Run the backend and inbox with Docker

With Docker Desktop running, start both services from the repository root:

```sh
docker compose up --build -d --wait
```

Open [backend health](http://127.0.0.1:3000/health) and [the Mailpit inbox](http://127.0.0.1:8025). Compose waits for Mailpit to be healthy before starting the backend; the backend has its own `/health` check. Docker supplies Node 22, so a host Node installation is only needed for direct backend development below.

The backend image compiles TypeScript in a build stage, then runs the compiled JavaScript with production dependencies as the non-root `node` user. The server listens on `0.0.0.0` inside the container so Docker can forward requests to it. The published host ports remain bound to `127.0.0.1`.

If a direct development server already uses port 3000, either stop that server or choose a different Docker host port:

```sh
BACKEND_PORT=3001 docker compose up --build -d --wait
```

Then use `http://127.0.0.1:3001/health`. This changes the host port only; the container still listens on port 3000.

Useful commands:

```sh
docker compose ps
docker compose logs backend
docker compose stop
```

## Run the backend directly for development

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

Direct execution defaults to your computer's loopback interface (`HOST=127.0.0.1`). The container sets `HOST=0.0.0.0` through its Dockerfile. Product routes and enquiry sending belong to later slices.

## Start the local email inbox

With Docker Desktop running, start Mailpit from the repository root:

```sh
docker compose up -d mailpit
```

Open [the Mailpit inbox](http://127.0.0.1:8025) in your browser.

| Address | Purpose |
| --- | --- |
| `127.0.0.1:1025` | SMTP server that accepts test email |
| `http://127.0.0.1:8025` | Browser inbox for inspecting captured messages |

Mailpit captures email locally; it does not deliver to an external mailbox. This setup needs no email-provider account or credentials. Both published ports bind to your computer's loopback interface. Inbox data is disposable; no persistent storage is configured.

The backend does not send email yet. A later enquiry slice will connect it to the SMTP address above.

To stop the inbox:

```sh
docker compose stop mailpit
```

## Catalogue data foundation

`backend/src/catalogue.ts` defines the product schema, its inferred TypeScript type, the product-source interface and a demo implementation. The demo source reads thirteen sample products across four categories from `backend/fixtures/products.json` and validates them with Zod. Sample copy, prices and ratings are illustrative.

This source is not connected to an HTTP route yet. Image paths in the fixture are data references; image files and their serving route will be added in a later chunk. Featured selection, filtering, live-source integration and Android requests are also separate chunks.

From `backend/`, run:

```sh
npm run typecheck
npm test
```

The test command compiles TypeScript and uses Node's built-in test runner against the compiled module. Tests cover loading the sample dataset, rejecting malformed product values and reporting missing fixture files. Fixture paths resolve relative to the module, so loading does not depend on the directory the server is launched from.

## Android backend address for later slices

The generated `BuildConfig.BASE_URL` defaults to `http://10.0.2.2:3000`. Inside the Android Emulator, `10.0.2.2` reaches your computer's loopback interface. This address is configured now; no HTTP client or network request exists yet.

Override it at build time when needed:

```sh
./gradlew assembleDebug -PapiBaseUrl=https://your-backend.example
```

For a physical device using a future local backend, a LAN address also requires explicitly making the backend reachable on that interface; the defaults above expose only loopback. Plain HTTP is permitted only by the debug manifest. A release build needs an HTTPS backend address.

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
├── Dockerfile               TypeScript build and production runtime stages
├── .dockerignore            Excludes local dependencies, build output and secrets
├── package.json             Node dependencies and scripts
├── tsconfig.json            TypeScript compiler settings
└── src/
    ├── app.ts               Express application and HTTP routes
    └── server.ts            Port configuration and server startup

compose.yaml                Local backend and Mailpit startup
```
