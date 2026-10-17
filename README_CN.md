<div align="center">

# 🚀 Twoyi Fork (两仪 Fork)

### *免 ROOT 的轻量级 Android 系统容器*

[![Build Status](https://github.com/sunandar3221/twoyi-fork/actions/workflows/build.yml/badge.svg)](https://github.com/sunandar3221/twoyi-fork/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/sunandar3221/twoyi-fork?color=blue&label=Latest%20Release)](https://github.com/sunandar3221/twoyi-fork/releases/latest)
[![Direct APK](https://img.shields.io/badge/Download-Direct%20APK-success?logo=android)](https://github.com/sunandar3221/twoyi-fork/releases/latest)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-orange.svg)](https://opensource.org/licenses/MPL-2.0)

<br/>

<img src="https://github.com/twoyi/twoyi/blob/main/assets/twoyi_screen.jpg?raw=true" alt="两仪截图" width="260px" style="border-radius: 12px; box-shadow: 0 4px 16px rgba(0,0,0,0.2);"/>

<p>
  <b>Twoyi Fork</b> 是基于原项目 <a href="https://github.com/tiann">weishu</a> 维护的增强分支版本。<br/>
  修复了闪退与卡顿问题、优化了深色/浅色模式下的弹窗文字对比度、支持自定义容器帧率，并提供 GitHub Actions 自动化直接 APK 下载。
</p>

</div>

---

## 🌟 此 Fork 的特性与修复

- ⚡ **直接下载 APK**：无需下载和解压 zip 包，可直接从 [GitHub Releases](https://github.com/sunandar3221/twoyi-fork/releases/latest) 下载 `.apk` 文件并在 Android 手机上安装。
- 📦 **双版本安装包**：
  - **`twoyi-slim-release.apk` (精简版，约 7 MB)**：超小安装包。首次启动时可通过 GitHub 在线自动下载镜像，也支持选择本地文件或自定义链接。
  - **`twoyi-offline-release.apk` (离线独立版，约 200 MB)**：内置完整 `rootfs.7z` 系统镜像，完全离线免联网安装。
- 🎯 **自定义容器帧率 (FPS)**：支持在设置中选择 **自动**（跟随设备屏幕刷新率，如 60Hz/90Hz/120Hz）、**30 FPS**、**45 FPS**、**60 FPS**、**90 FPS** 或 **120 FPS**。
- 🌓 **深色/浅色模式弹窗完美对比度**：
  - **深色模式**：纯黑背景 (`#000000`) 配合纯白文字 (`#FFFFFF`)。
  - **浅色模式**：纯白背景 (`#FFFFFF`) 配合纯黑文字 (`#000000`)。
  - 彻底解决应用管理器与安装弹窗中文字泛白或看不清的问题。
- 🛡️ **稳定性提升与防闪退**：
  - 忽略 Unix 域套接字的 `SIGPIPE` 信号，避免进程意外终止。
  - 安全退出触摸与按键事件循环，杜绝线程泄漏与 100% CPU 导致的卡死。
  - 完善窗口指针生命周期释放及引导日志渲染线程停止逻辑。
- 🛠️ **可调容器参数**：支持在设置中直接调整容器屏幕密度 (DPI) 和启动超时时间。

---

## 📥 下载地址

前往 **[GitHub Releases 最新发布页](https://github.com/sunandar3221/twoyi-fork/releases/latest)** 下载：

| 版本 | 文件名 | 体积 | 说明 |
| :--- | :--- | :--- | :--- |
| **精简版 (推荐)** | [`twoyi-slim-release.apk`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~7 MB | 轻量小巧，首次启动在线下载容器镜像。 |
| **离线版** | [`twoyi-offline-release.apk`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~200 MB | 完整独立版，内置 ROM，无需额外联网。 |
| **系统镜像** | [`rootfs.7z`](https://github.com/sunandar3221/twoyi-fork/releases/latest) | ~85 MB | 官方两仪容器 rootfs 镜像压缩包。 |

---

## 🤝 致谢

- **原作者**：[weishu (tiann)](https://github.com/tiann) 创建并开源了两仪架构。
- **Fork 维护**：[sunandar3221](https://github.com/sunandar3221)。

## 📄 许可证

本项目采用 [Mozilla Public License 2.0 (MPL-2.0)](LICENSE) 开源协议。
