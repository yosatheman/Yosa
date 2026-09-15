# Deep Current 🌊

High-performance encrypted tunneling and network proxy application for Android, built with modern Jetpack Compose and Material Design 3.

---

## ⚡ Features

- **Multi-Protocol Transit**: Supports SSH, SSL/TLS, WebSocket, DNS Tunneling, and HTTP-CONNECT proxy configurations.
- **Dynamic Theming**: True dark canvas with 5 swappable accents (Cyan, Violet, Amber, Rose, Mint) and fluid typography scaling.
- **Interactive Connect Orb**: Visual state-reactive 220dp hero connect button with real-time sweep, pulse, and drain animations.
- **Traffic Telemetry & Latency**: Real-time RTT sparklines, 3-sample latency pre-flight pings, and live throughput meters.
- **Payload Injection Engine**: Token expansion engine (`[crlf]`, `[host]`, `[port]`, `[rotate]`, `[random=N]`, `[ua]`) with syntax highlighting and categorized payload library.
- **Split Tunneling & Rules**: Per-app routing bypass and granular MTU/DNS configuration.
- **Multi-Format Portability**: Full support for native JSON, HTTP Custom (`.hc`), and standard `ssh://` URI formats, with integrated QR code generation and sharing.

---

## 🛠️ GitHub Actions CI / Automated Builds

This repository includes a continuous integration workflow located at [`.github/workflows/build.yml`](.github/workflows/build.yml).

### Automated Pipeline:
1. **Triggers**: Runs on every `push` and `pull_request` to `main`/`master`, or manually via **Run workflow** (`workflow_dispatch`).
2. **Environment**: Runs on `ubuntu-latest` with JDK 21 (Temurin).
3. **Tasks**:
   - Executes JVM unit tests: `./gradlew testDebugUnitTest`
   - Compiles debug APK: `./gradlew :app:assembleDebug`
4. **Artifacts**: Uploads the generated APK (`deep-current-debug-apk`) and test reports as downloadable GitHub build artifacts.

---

## 🚀 How to Push to GitHub from Google AI Studio

1. In the Google AI Studio project toolbar or settings menu, click **Export / Push to GitHub**.
2. Connect your GitHub account and select or create the target repository.
3. Once pushed, navigate to the **Actions** tab in your GitHub repository to watch the build pipeline run automatically.
4. Download the built APK directly from the **Artifacts** section of the completed workflow run.

---

## 💻 Local Building

Ensure you have **JDK 21** installed.

```bash
# Clone the repository
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>

# Ensure gradlew has execution rights (macOS / Linux)
chmod +x gradlew

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew :app:assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
