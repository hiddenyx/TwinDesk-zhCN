# Windows 桌面版汉化补丁

TwinDesk 桌面端（Electron）没有多语言支持，本补丁在**不修改应用安装包**的前提下，
通过注入运行时翻译层把界面文本换成中文，并可一键还原。

## 组成

| 文件 | 作用 |
|---|---|
| `patch/zh-cn/zh-dict.js` | 渲染进程词典：949 条整句 + 296 条带占位符的模板 |
| `patch/zh-cn/zh-engine.js` | 渲染进程引擎：遍历 DOM 文本/属性并替换，MutationObserver 跟踪动态内容 |
| `patch/zh-cn/zh-main.js` | 主进程钩子：托盘菜单、原生对话框、通知、窗口标题 |
| `patch/zh-cn/zh-dict-main.json` | 主进程用的同一份词典 |
| `scripts/apply.ps1` | 备份原始文件 → 注入 → 重启应用（可重复执行） |
| `scripts/undo.ps1` | 还原所有原始文件并删除注入目录 |
| `scripts/build.js` | 由 `trans/batch*.json` 合并生成词典 |
| `scripts/manual.json` | 人工补充/修正的整句与模板 |
| `scripts/GUIDE.md` | 翻译规范与术语表 |
| `trans/batch*.json` | 翻译批次原始数据（英→中） |
| `backup/` | 被修改的原始文件（还原用） |

## 使用

需要管理员权限（目标目录 `C:\Program Files\TwinDesk`）：

```powershell
# 打补丁并重启 TwinDesk
powershell -ExecutionPolicy Bypass -File .\scripts\apply.ps1

# 还原英文
powershell -ExecutionPolicy Bypass -File .\scripts\undo.ps1
```

注：
- TwinDesk 升级/重装会覆盖 `resources/app`，补丁会失效，需要重新执行 `apply.ps1`。
- 补丁只在 `resources/app/zh-cn/` 与 11 个 HTML、2 个 main.js 的注入点上改动，业务逻辑不改。
- 已知限制：个别动态拼接句仍可能中英混排（逐串替换方案的固有限制）。