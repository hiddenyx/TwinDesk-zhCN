# TwinDesk 2.4.0 简体中文（非官方备份）

> 非官方汉化存档，与 TwinDesk 作者无关。上游：https://github.com/InukaAsith/TwinDesk（GPL-3.0）

## 资产

| 文件 | 说明 | SHA256 |
|---|---|---|
| `TwinDesk-2.4.0-sourcebuild-zhCN.apk` | 从 v2.4.0 源码编译的中文版（debug 签名） | `25266dba17123f25c77ee73da0fb88e970227c1c4819e38c3789a628808b9937` |
| `TwinDesk-2.4.0-sourcebuild-en.apk` | 同源码编译的英文原版，用于验证可复现性 | `7835885b9093afad4e4076a4c2f677f3bac810c982f0ece9c21533c263a4fe42` |
| `twin-desktop-zhCN-patch.zip` | Windows 桌面版运行时汉化补丁（词典/引擎/脚本/原始备份） | 见 zip 内 README |

## 关键说明

- **可复现性**：英文版与官方 Release 的 `classes.dex`、`classes2.dex`、`resources.arsc`、`AndroidManifest.xml`、`lib/*.so` 内容 SHA256 完全一致（仅签名不同），证明本构建只改了字符串。详见仓库 `docs/reproducibility.md`。
- **安装**：中文 APK 与官方版签名不同，需先卸载官方版（配对信息会丢）；安装后按向导授予通知、后台运行、显示指针、附近的设备等权限。
- **Windows 补丁**：管理员运行 `scripts/apply.ps1` 打补丁，`scripts/undo.ps1` 还原。
- 本版本为个人备份，不提供更新支持；上游更新后补丁可能失效。