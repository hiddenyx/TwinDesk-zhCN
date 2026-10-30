# TwinDesk 简体中文（非官方备份仓库）

> ⚠️ **非官方项目**。本仓库只是个人备份 + 汉化补丁/构建产物存档，**与 TwinDesk 作者无关**，不代表官方发布。
> 上游项目：[InukaAsith/TwinDesk](https://github.com/InukaAsith/TwinDesk)（GPL-3.0）

## 这是什么

[TwinDesk](https://github.com/InukaAsith/TwinDesk) 2.4.0 的简体中文改造存档，包含两套东西：

| 目录 | 内容 |
|---|---|
| `android/` | **从源码编译**的 Android 中文版：完整修改后源码 + 汉化脚本 + 构建产物 APK |
| `windows/` | Windows 桌面版的**运行时汉化补丁**（不修改安装包，注入中文词典/引擎，可一键还原） |
| `docs/` | 构建说明、可复现性验证、截图 |

## Android（源码编译版）

- 基线：上游 tag `v2.4.0`
- 做法：在 Kotlin 源码层面替换 UI 字符串（共约 600+ 处），不改协议字段/JSON key/SQL/日志；另修复了 `⌃⌥` 等键盘符号在安卓字体下显示为乱码的问题（改为 `Ctrl+Alt+`）
- 产物：`android/artifacts/TwinDesk-2.4.0-sourcebuild-zhCN.apk`
  - SHA256：`25266dba17123f25c77ee73da0fb88e970227c1c4819e38c3789a628808b9937`
  - 包名 `com.twindesk.android`，versionName 2.4.0，minSdk 29 / targetSdk 36
  - 使用构建机 debug 密钥签名（**非官方签名**）
- 英文原版对照：`android/artifacts/TwinDesk-2.4.0-sourcebuild-en.apk`（本地编译的英文版，用于验证可复现性）

### 可复现性

本机从 `v2.4.0` 源码编译出的英文 APK，与官方 Release 包逐项对比，以下内容 **SHA256 完全一致**：
`classes.dex`、`classes2.dex`、`resources.arsc`、`AndroidManifest.xml`、`lib/*.so`
（仅签名与 zip 容器元数据不同）。详见 [docs/reproducibility.md](docs/reproducibility.md)。

### 自己编译

```powershell
# 需要 JDK 21 + Android SDK（platforms;android-37.0、build-tools;37.0.0）
$env:JAVA_HOME = "C:\path\to\jdk-21"
cd android
.\gradlew assembleStandardRelease
# 产物：app\build\outputs\apk\standard\release\app-standard-release.apk
```

构建配置改动：`app/build.gradle.kts` 里把 buildToolsVersion 固定为 `37.0.0`（上游默认 36.0.0）。

### 安装

1. 卸载手机上的官方 TwinDesk（签名不同，不能覆盖安装；配对数据会丢）
2. 安装本仓库的 APK，允许「安装未知应用」
3. 首次打开按向导授予权限（通知、后台运行、显示指针、附近的设备等）

> 手机安全中心/Play Protect 可能提示「来历不明/风险应用」——这是自签名重打包的常规提示。

## Windows（运行时汉化补丁）

- `windows/patch/zh-cn/`：中文词典（949 条整句 + 296 条模板）、渲染进程引擎、主进程钩子
- `windows/scripts/apply.ps1`：备份原始文件 → 注入 → 重启应用（可重复执行，幂等）
- `windows/scripts/undo.ps1`：一键还原英文
- `windows/backup/`：被修改的原始文件备份（用于还原）
- `windows/trans/`：翻译批次源文件（英→中对照）

用法（需管理员，TwinDesk 默认安装在 `C:\Program Files\TwinDesk`）：

```powershell
powershell -ExecutionPolicy Bypass -File .\windows\scripts\apply.ps1     # 打补丁并重启
powershell -ExecutionPolicy Bypass -File .\windows\scripts\undo.ps1      # 还原英文
```

> 补丁只替换界面文本，不改业务逻辑；应用更新后会被覆盖，需要重新打。

## 许可

- 本项目是 TwinDesk 的衍生作品，整体按 **GPL-3.0** 发布，见 [LICENSE](LICENSE)。
- 上游版权归原作者（InukaAsith）所有；本仓库保留其版权声明与第三方许可声明，见 [NOTICE.md](NOTICE.md) 与 [docs/THIRD_PARTY_NOTICES.txt](docs/THIRD_PARTY_NOTICES.txt)。
- 未获得使用 TwinDesk 名称/图标作为官方标识的任何授权；本仓库不暗示官方背书。

## 免责声明

- 仅供个人备份与学习；按「现状」提供，不提供任何担保。
- 修改版可能与上游后续版本不兼容；升级/重装可能丢失配对信息。
- TwinDesk 具备屏幕共享、输入控制、通知/短信等敏感能力，请只在可信设备与网络中使用。