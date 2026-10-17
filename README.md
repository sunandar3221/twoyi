<div align="center">

# 🚀 Twoyi Fork (两仪 Fork)

### *A Lightweight, Rootless Android Container for Android*

[![Build Status](https://github.com/sunandar3221/twoyi-fork/actions/workflows/build.yml/badge.svg)](https://github.com/sunandar3221/twoyi-fork/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/sunandar3221/twoyi-fork?color=blue&label=Latest%20Release)](https://github.com/sunandar3221/twoyi-fork/releases/latest)
[![Direct APK](https://img.shields.io/badge/Download-Direct%20APK-success?logo=android)](https://github.com/sunandar3221/twoyi-fork/releases/latest)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-orange.svg)](https://opensource.org/licenses/MPL-2.0)

<br/>

<img src="https://github.com/twoyi/twoyi/blob/main/assets/twoyi_screen.jpg?raw=true" alt="Twoyi Screenshot" width="260px" style="border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.2);"/>

<p>
  <b>Twoyi Fork</b> is an actively maintained continuation of the original Twoyi project by <a href="https://github.com/tiann">weishu</a>.<br/>
  It brings stability fixes, native crash prevention, dark/light theme visibility improvements, configurable frame rates, and automated direct APK releases.
</p>

</div>

---

## 🌟 Highlights & New Features in this Fork

- ⚡ **Direct APK Downloads**: No zip archives! Download ready-to-install `.apk` files directly to your Android device from [GitHub Releases](https://github.com/sunandar3221/twoyi-fork/releases/latest).
- 📦 **Dual Release Packages**:
  - **`twoyi-slim-release.apk` (~7 MB)**: Ultra-compact edition. Downloads the container OS (`rootfs.7z`, ~85MB) automatically inside the app via GitHub Releases, with fallback to local file selection or custom URL.
  - **`twoyi-offline-release.apk` (~200 MB)**: Standalone complete edition with the container OS pre-bundled for 100% offline installation.
- 🎯 **Configurable Frame Rate (FPS)**: Choose between **Auto** (dynamically matches device display refresh rate, e.g. 60Hz, 90Hz, 120Hz), **30 FPS** (Power Saver), **45 FPS** (Balanced), **60 FPS** (Smooth), **90 FPS** (High Refresh Rate), or **120 FPS** (Ultra Smooth).
- 🌓 **Dynamic Dark & Light Mode Dialogs**:
  - Automatically adapts to system UI theme:
    - **Dark Mode**: Dialogs feature a solid black background (`#000000`) with crisp, bold white text (`#FFFFFF`).
    - **Light Mode**: Dialogs feature a solid white background (`#FFFFFF`) with solid black text (`#000000`).
  - No more faint, washed-out, or invisible text in the App Manager or installation dialogs.
- 🛡️ **Hang & Native Crash Prevention**:
  - **Touch & Multi-Touch Pipeline Fix**: Properly emits `BTN_TOUCH` and `BTN_TOOL_FINGER` release (`0`) on touch up, preventing Android guest `InputDispatcher` from hanging or freezing window gesture queues.
  - **Batched Socket Writes**: Batches touch/key event writes to reduce ptrace context-switch syscall storms by >90% during rapid drags/swipes.
  - **Panic-Safe Native JNI**: Enclosed touch and key handling in Rust `catch_unwind` with safe pointer bounds checking (up to 10 multi-touch slots).
  - **Deadlock-Free State Machine**: Replaced `CyclicBarrier` with non-blocking `CountDownLatch` in `TwoyiStatusManager` to prevent boot/reboot hangs.
  - **SurfaceFlinger Optimization**: Enabled `disable_backpressure` and `latch_unsignaled` to prevent container frame rendering stalls.
  - Ignores unhandled `SIGPIPE` signals on native Unix domain sockets to prevent sudden silent app exits.
  - Safely terminates touch and key input event loops upon socket disconnection, eliminating CPU 100% thread leaks.
  - Added null safety checks and proper `ANativeWindow_release` handling on window lifecycle changes.
  - Automatically terminates boot log rendering background threads once startup completes.
- 🛠️ **Customizable Container Settings**: Customize container DPI (density) and boot timeout directly from Settings.
- 🤖 **Automated GitHub Actions CI/CD**: Clean compilation workflow compiling Rust native library (`libtwoyi.so`) and Gradle APKs with automated direct release publishing.

---

## 📥 Download Links

Download the latest release directly from **[GitHub Releases](https://github.com/sunandar3221/twoyi-fork/releases/latest)**:

| Edition | File Name | Size | Description |
| :--- | :--- | :--- | :--- |
| **Slim (Recommended)** | [`twoyi-slim-release.apk`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~7 MB | Lightweight APK. Downloads or extracts OS ROM on first launch. |
| **Offline Standalone** | [`twoyi-offline-release.apk`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~200 MB | Full offline package. Contains internal OS ROM inside APK. |
| **OS Package** | [`rootfs.7z`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~85 MB | Official container rootfs archive for manual installation. |

---

## 📖 How It Works

Twoyi is a rootless system-level container running a full Android 8.1 userland inside an Android app:
- **No root required**: Operates within normal Android app sandbox permissions.
- **Fast Startup**: Boots in seconds once installed.
- **App Isolation**: Install and test applications inside an isolated container without affecting host system data.
- **Compatibility**: Supports host devices running Android 8.1 up to Android 12+.

---

## ⚙️ Building from Source

This project utilizes GitHub Actions CI for all compilation tasks. To build manually:

### Prerequisites
1. **JDK 11**
2. **Android NDK r21e**
3. **Rust Toolchain** with `aarch64-linux-android` target
4. **cargo-xdk**:
   ```bash
   cargo install --git https://github.com/tiann/cargo-xdk
   ```

### Build Steps
```bash
# 1. Build native Rust library
cd app/rs
cargo xdk -t arm64-v8a -o ../src/main/jniLibs build --release
cd ../..

# 2. Build APK using Gradle
./gradlew assembleRelease
```

---

## 🤝 Credits & Acknowledgements

- **Original Creator**: [weishu (tiann)](https://github.com/tiann) for the original [twoyi/twoyi](https://github.com/twoyi/twoyi) architecture.
- **Fork Maintainer**: [sunandar3221](https://github.com/sunandar3221) for bugfixes, stability enhancements, and automated CI maintenance.

## 📄 License

This project is licensed under the [Mozilla Public License 2.0 (MPL-2.0)](LICENSE).
