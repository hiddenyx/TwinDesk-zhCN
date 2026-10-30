# TwinDesk zh-CN 翻译任务说明（Windows 桌面端 UI）

我们要给开源软件 **TwinDesk 2.4.0**（跨设备互联工具：把 Mac / Windows / 安卓手机当成同一张桌面用，共享屏幕、鼠标键盘、剪贴板、文件）做一个简体中文语言包。
界面文本目前全部硬编码英文，需要翻译成**简体中文（大陆用语）**。

## 你的输入 / 输出

- 输入：`F:\deep\_twindesk_i18n\batches\batchN.json`（N 已分配给你）
  每条记录：`en`（英文原文）、`kind`（text/attr/literal/template）、`example`（出处文件）、`line`、`ctx`（代码上下文，方便判断语境）、`refs`（同串出现次数）。
- 输出：`F:\deep\_twindesk_i18n\trans\batchN.json`
  格式：`{ "<en 原样>": "<中文译文>" }`，必须覆盖输入里的**全部**条目（键与输入完全一致，一个不多一个不少）。
- 只写你自己的输出文件，不要修改其他任何文件。

## 硬性规则

1. **占位符必须原样保留并保序**：`{0}` `{1}` `{2}`、`%s` `%d`、`${...}` 都不要翻译、不要改动、不要调换顺序。中文语序需要时可重排占位符位置，但占位符本身必须完整保留。
2. **专有名词不译**：TwinDesk、Mac、MacBook、iPhone、iPad、Windows、Android、Linux、ADB、Mesh、Wi-Fi、Bluetooth、USB、HDMI、KVM、TCP、UDP、H.264、HEVC、AirPlay 等保持原样。产品名 TwinDesk 永远不译。
3. **纯符号/单位/代码串保持原样**：例如 `OK` 译 `确定`，但 `ms`、`fps`、`MB/s`、`Ctrl+Alt+C`、`100%`、`v2.4.0`、`http://...`、`*.png`、`#f00` 这类不改动（值 = 原样）。若判断某条其实是代码标识符（如 `GET`、`base64`、`localhost`、`adb`），也原样返回。
4. **语气**：桌面软件 UI，简洁、专业、口语自然。按钮 2–5 字（如 "Connect" → "连接"，"Cancel" → "取消"，"Done" → "完成"）。说明性句子用全角标点（，。：；？），按钮/标签不带标点。
5. **保留原有标点风格**：原文以省略号 "…" 结尾的译文也以 "…" 结尾；原文是问句的译文也保留问号。
6. **不要添加原文没有的信息**（不加解释、不加"点击"之类不存在的动作），不要删减语义；长度尽量接近原文，避免撑破按钮。
7. **大小写与首字母**：中文不用大写概念；英文原文里的缩写（如 "PC"、"OK"）按惯例处理（PC 保留）。
8. 若同一条英文在不同上下文有不同含义（看 `ctx`），选**最通用**的那个译法，并在一条里用最贴切的表达（不要用斜杠堆两个译法）。
9. 译文里不要出现英文引号包裹整句；专有名词内的空格保留（`Mac and Windows` → `Mac 和 Windows`，中西文之间加空格）。

## 术语表（保持一致，务必遵守）

| 英文 | 统一中文 |
|---|---|
| pair / pairing / paired / not paired | 配对 / 配对中 / 已配对 / 未配对 |
| code（配对场景） | 配对码 |
| peer | 对端设备 |
| device / devices | 设备 |
| computer | 电脑（"this computer" → 本机） |
| phone / tablet | 手机 / 平板 |
| screen | 屏幕 |
| display / monitor | 显示器 |
| extended display | 扩展显示器 |
| mirror / mirrored | 镜像 |
| virtual display | 虚拟显示器 |
| cursor | 光标 |
| pointer | 指针 |
| clipboard | 剪贴板 |
| keyboard | 键盘 |
| mouse | 鼠标 |
| hotkey / shortcut | 快捷键 |
| app / apps | 应用 |
| window / windows | 窗口 |
| file / files | 文件 |
| folder | 文件夹 |
| share / sharing | 共享 |
| session | 会话 |
| connection / connected / disconnected / connecting | 连接 / 已连接 / 已断开 / 连接中 |
| camera | 摄像头 |
| microphone / mic | 麦克风 |
| speaker / audio / sound | 扬声器 / 音频 / 声音 |
| notification(s) | 通知 |
| terminal | 终端 |
| permission | 权限 |
| administrator | 管理员 |
| firewall | 防火墙 |
| update / updater | 更新 / 更新程序 |
| settings | 设置 |
| General | 常规 |
| Overview | 概览 |
| About | 关于 |
| version | 版本 |
| quality | 画质 |
| bitrate | 码率 |
| frame rate | 帧率 |
| fullscreen | 全屏 |
| tray | 系统托盘 |
| quit / exit | 退出 |
| launch at login / start with Windows | 开机启动 |
| Wireless debugging | 无线调试 |
| Messages / SMS | 消息 / 短信 |
| Call / calls | 通话 |
| Wake-on-LAN | 网络唤醒 |
| gamepad | 手柄 |
| stylus | 手写笔 |
| battery | 电池 |
| mount / unmount | 挂载 / 卸载 |
| transfer | 传输 |
| receive / received | 接收 / 已接收 |
| send / sent | 发送 / 已发送 |
| drag and drop | 拖放 |
| tile | 平铺 |
| mesh | Mesh（保留英文） |

## 完成后

报告：写入文件路径、条目数、你判断为"保持原样（非 UI 文本）"的条目数。不要输出整份 JSON 到聊天里，只写文件。