# NOTICE / 归属声明

## 上游项目

- **TwinDesk** — https://github.com/InukaAsith/TwinDesk
- Copyright (C) 2026 inukaasith（及贡献者，如 Erdoğan Yeşil 等，见上游各文件头）
- 许可证：GNU General Public License v3.0（GPL-3.0），全文见 `LICENSE`

本仓库包含对 TwinDesk 2.4.0 的修改版源码（Android）与运行时汉化补丁（Windows），
属于 GPL-3.0 意义上的衍生作品，按 GPL-3.0 分发。

## 与官方的关系

- 本仓库为**非官方**备份/汉化存档，与上游作者无隶属或背书关系。
- 未使用 TwinDesk 名称或图标宣称官方发布；文件名中保留 “TwinDesk” 仅用于说明适用对象。
- 官方发布渠道仅限上游 GitHub Releases；请优先使用官方版本。

## 第三方组件

Android APK 内含上游项目使用的第三方库，其许可与声明随 APK 原样保留，
完整清单见 `docs/THIRD_PARTY_NOTICES.txt`（取自上游仓库），包括但不限于：

- scrcpy-server（Apache-2.0）
- OkHttp（Apache-2.0）
- DeskPad（MIT）
- AndroidX / Jetpack Compose（Apache-2.0）
- Kotlin 运行时（Apache-2.0）

Windows 汉化补丁仅为运行时注入的词典/引擎/主进程钩子，不重新分发 TwinDesk 安装包本体。

## 修改说明（GPL-3.0 §5）

- Android：修改 `android/app/src/main/java/com/twindesk/android/*.kt` 中的界面字符串；
  构建配置中固定 `buildToolsVersion = "37.0.0"`。修改脚本见 `android/patches/`。
- Windows：通过 Electron 渲染进程/主进程钩子替换界面文本；不改应用业务逻辑。
  见 `windows/patch/` 与 `windows/scripts/`。
- 完整修改后源码：`android/` 目录；补丁脚本与对照表：`android/patches/`、`windows/trans/`。

## 构建环境说明

- Android 构建使用 JDK 21、Android SDK Platform 37.0、Build-Tools 37.0.0、Gradle 9.8.1。
- 构建产物用构建机的 **debug 密钥**签名（非官方签名、非生产密钥）。