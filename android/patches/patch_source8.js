// 第八轮：收尾剩余英文界面文案 + 特殊键位符号（⌃⌥⌘ 在安卓上显示成乱码）
const fs = require('fs');
const path = require('path');
const SRC = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java';

// 英文（含 {0} 占位）-> 中文
const T = {
  '{0} uses it for this device’s screen, its mouse and keyboard here, and sound.': '{0} 用它来显示这块屏幕、在此使用鼠标键盘并传输声音。',
  ' · {0} ms': ' · {0} ms',
  '{0} ms': '{0} ms',
  '{0} is only available over Wi-Fi Direct. Connecting…': '{0} 仅支持 Wi-Fi Direct。正在连接…',
  '{0} is only available over Wi-Fi Direct (or the same Wi-Fi network): you’re connected over Bluetooth.': '{0} 仅支持 Wi-Fi Direct（或同一 Wi-Fi 网络）：你当前通过蓝牙连接。',
  'TwinDesk on {0} needs to be up to date for {1}.': '{0} 上的 TwinDesk 需为最新版本才能使用{1}。',
  'File access needs Android 11 or later. Mount on {0} works through ADB instead.': '文件访问需要 Android 11 或更高版本。可以改用 ADB 在 {0} 上挂载。',
  'Allow photos and videos, so {0} can show them.': '允许访问照片和视频，这样 {0} 才能显示它们。',
  'Only the photos you chose show on {0}.': '只有你选择的照片会显示在 {0} 上。',
  'Some of {0} needs more: allow the rest to use all of it.': '{0} 还需要更多权限：请允许其余权限以完整使用。',
  'On: {0} from this phone on {1}.': '开启：在这台手机上使用{0}，显示在 {1}。',
  'On {0}': '在 {0} 上',
  'The number pad, contacts and recent calls on {0}’s Calls page': '{0} 的通话页上有数字键盘、联系人和最近通话',
  'Sound on {0}': '在 {0} 上播放声音',
  'Allow Nearby devices so TwinDesk sees this phone’s pairing with {0}': '允许「附近的设备」，让 TwinDesk 能看到这台手机与 {0} 的配对',
  'Pairing… Confirm the code here and on {0}': '配对中… 请在此处和 {0} 上确认配对码',
  'Forget {0} in Bluetooth settings here (Android keeps the old pairing for itself), then pair them again there': '请在本机蓝牙设置中取消与 {0} 的配对（Android 会自己保留旧配对），然后在 {0} 上重新配对',
  'Paired with {0}: calls can play there': '已与 {0} 配对：可以在那边接听通话',
  'Waiting for {0}…': '正在等待 {0}…',
  'Not paired with {0} over classic Bluetooth: pair them in Bluetooth settings (here: Pair new device, or Add device on {0}). TwinDesk connects once they are': '尚未与 {0} 建立经典蓝牙配对：请在蓝牙设置中配对（本机：配对新设备；{0}：添加设备）。配对完成后 TwinDesk 会自动连接',
  Conversations: '会话',
  'Texts and MMS pictures on {0}’s Messages page': '{0} 的短信页可查看短信和彩信图片',
  'Counted on the Messages tab on {0}': '在 {0} 的「短信」标签中统计',
  'Photos on {0}': '在 {0} 上查看照片',
  'Newest first on {0}’s Photos page': '{0} 的照片页按最新优先排序',
  'Copies go to {0}’s Downloads': '复制的内容会保存到 {0} 的「下载」中',
  'Phone Files on {0}': '{0} 上的手机文件',
  'Phone Files': '手机文件',
  'Folders, Save to {0}, Upload, New folder, Rename, Delete': '文件夹、保存到 {0}、上传、新建文件夹、重命名、删除',
  'No ADB': '无需 ADB',
  'TwinDesk reads and writes the files itself with All files access. Mount on {0} is the ADB way': 'TwinDesk 通过「所有文件访问」权限自行读写文件。在 {0} 上挂载是走 ADB 的方式',
  'Share a folder with {0}': '与 {0} 共享文件夹',
  '{0}’s shared folder': '{0} 的共享文件夹',
  'Off: switch it on to see a folder of {0} in the Files app': '关闭：开启后可在「文件」应用中看到 {0} 的文件夹',
  'TwinDesk Camera': 'TwinDesk 摄像头',
  'This camera on {0}': '把此摄像头显示在 {0} 上',
  Front: '前置',
  '{0}’s Virtual camera is on: apps there see this camera as “TwinDesk Camera”.': '{0} 的虚拟摄像头已开启：那边的应用会把这台摄像头识别为「TwinDesk 摄像头」。',
  'Turn on Virtual camera in TwinDesk’s Camera page on {0} to use this camera as a real camera in its apps (Zoom, Teams, the browser…).': '在 {0} 上 TwinDesk 的「摄像头」页开启虚拟摄像头，即可把此摄像头作为那边应用（Zoom、Teams、浏览器…）可用的真实摄像头。',
  '{0} shares a second screen the size of this one (Extend).': '{0} 共享了一块与本机同样大小的第二屏幕（扩展）。',
  '{0} shares its own screen (Mirror).': '{0} 正在共享它自己的屏幕（镜像）。',
  '{0} starts and switches its sharing itself': '{0} 自行开始和切换共享',
  Sharing: '共享',
  Touch: '触摸',
  Click: '单击',
  Drag: '拖拽',
  'Drag (select, move windows)': '拖拽（选择、移动窗口）',
  'Input is off': '输入已关闭',
  '{0} doesn’t take input from here (its Share screen › Receive input)': '{0} 未接受本机的输入（其「共享屏幕」›「接收输入」）',
  'This build of TwinDesk can’t use Accessibility: Android’s Play Protect blocks downloaded apps that can.': '此版本的 TwinDesk 无法使用辅助功能：Android 的 Play Protect 会拦截具备该能力的下载应用。',
  'Use wireless debugging, or once it’s set up, {0} installs TwinDesk with notifications, which can (on {0}, Phone page › Install).': '请使用无线调试；配置好后，{0} 会安装带通知功能的 TwinDesk，它可以使用（在 {0} 的「手机」页 ›「安装」）。',
  'Allow TwinDesk in Accessibility, so {0}’s pointer can tap and type here.': '请在辅助功能中允许 TwinDesk，这样 {0} 的指针就能在这里点按和输入。',
  '{0} moves the pointer here through wireless debugging, which is off. Or choose Accessibility below.': '{0} 通过无线调试把指针移到这里，但无线调试已关闭。也可以在下面选择辅助功能。',
  'Allow “Show the pointer” in General › Setup to see {0}’s pointer here.': '请在「常规 › 设置」中允许「显示指针」，以便在这里看到 {0} 的指针。',
  Feel: '手感',
  '{0}’s keyboard types here while its pointer is here.': '{0} 的键盘在其指针位于此处时可在此输入。',
  Hotkey: '快捷键',
  'Double-tap on {0} to turn cursor sharing on or off. Set it in TwinDesk on {0}': '在 {0} 上双击可开启或关闭光标共享。可在 {0} 的 TwinDesk 中设置',
  Typing: '输入',
  'A keyboard on this device types on {0}. The Keyboard button opens the soft keyboard': '本机的键盘可在 {0} 上输入。「键盘」按钮会打开软键盘',
  'Use this device as {0}’s touchpad and keyboard, or as a gamepad.': '把本机用作 {0} 的触控板和键盘，或用作手柄。',
  '{0} doesn’t answer the remote: update TwinDesk there.': '{0} 没有响应遥控：请更新那边的 TwinDesk。',
  '{0} doesn’t take input from here. On {0}, turn on Remote in TwinDesk.': '{0} 不接受本机的输入。请在 {0} 的 TwinDesk 中开启「遥控」。',
  Gamepad: '手柄',
  'Even pointer motion on {0}, about a frame later. Off: each move the moment it arrives': '对 {0} 的指针移动做平滑处理，约晚一帧。关闭：每次移动立即生效',
  'A game controller for any computer over Bluetooth, the Mac too: no driver and no TwinDesk needed there.': '通过蓝牙把本机当作任意电脑（包括 Mac）的游戏手柄：那边无需驱动，也无需 TwinDesk。',
  'Copies on {0} land here': '在 {0} 上复制的内容会出现在这里',
  'Copies here go to {0}': '在这里复制的内容会发送到 {0}',
  'Pictures and screenshots up to 10 MB, both ways, with Clipboard › Pictures on on {0} too. Not over Bluetooth': '双向同步不超过 10 MB 的图片和截图，且需在 {0} 上也开启「剪贴板 › 图片」。不支持蓝牙',
  'Off on {0}': '在 {0} 上已关闭',
  'Screenshots go to {0}': '截图会发送到 {0}',
  'Each one taken here lands on {0}’s clipboard: paste it there': '每次在本机截图都会进入 {0} 的剪贴板：在那边粘贴即可',
  '{0} installs TwinDesk with notifications for you once wireless debugging is set up: on {0}, Phone page › Install. Your settings and pairing stay.': '无线调试配置好后，{0} 会为你安装带通知功能的 TwinDesk：在 {0} 的「手机」页 ›「安装」。设置和配对都会保留。',
  'Allow Notification access so this device’s notifications can go to {0}.': '请允许通知使用权，这样本机的通知就能发送到 {0}。',
  'Show this device’s notifications on {0}': '在 {0} 上显示本机的通知',
  'Only the app and title go to {0}': '只把应用名和标题发送到 {0}',
  'Set it up first, then install from {0}': '请先完成设置，然后从 {0} 安装',
  'Show {0}’s notifications here': '在此显示 {0} 的通知',
  'Copied on {0}': '在 {0} 上复制',
  Copy: '复制',
  '{0} · {1}%': '{0} · {1}%',
  Downloads: '下载',
  'Play {0}’s sound here': '在此播放 {0} 的声音',
  Playing: '播放中',
  'Play this device’s sound on {0}': '在 {0} 上播放本机的声音',
  'Paired with {0}: this screen and sound there, its mouse and keyboard here': '已与 {0} 配对：屏幕和声音在那边，鼠标键盘在这边',
  'Paired with {0}, reconnecting…': '已与 {0} 配对，正在重新连接…',
  'On. Pair it with {0} once (the guide walks you through it)': '已开启。与 {0} 配对一次即可（向导会引导你）',
  'For this screen and sound on {0}, and its mouse and keyboard here': '让屏幕和声音显示在 {0}，并在此使用那边的鼠标键盘',
  'Wireless debugging stays on after {0} disconnects': '{0} 断开后无线调试保持开启',
  'Through wireless debugging. Without it: comes with TwinDesk with notifications, which {0} installs (its Phone page)': '通过无线调试。若不可用：带通知功能的 TwinDesk 自带该能力，{0} 会安装（其「手机」页）',
  'Draws {0}’s pointer over other apps': '在其他应用之上显示 {0} 的指针',
  'Your wallpaper on {0}, Files and shared folders': '把壁纸显示到 {0}、文件与共享文件夹',
  'On the same Wi-Fi network as {0} (not Wi-Fi Direct), Bluetooth goes off until the connection ends': '当与 {0} 在同一 Wi-Fi（非 Wi-Fi Direct）时，蓝牙会关闭直到连接结束',
  'Joining {0}’s Wi-Fi…': '正在加入 {0} 的 Wi-Fi…',
  'On a direct Wi-Fi link with {0}': '已与 {0} 建立 Wi-Fi 直连',
  Online: '在线',
  Portfolio: '个人主页',
  Releases: '全部版本',
  Read: '已读',
  Network: '网络',
  'Once, so {0} can show this screen and use its mouse and keyboard here': '只需一次，之后 {0} 就能显示这块屏幕并在此使用鼠标键盘',
  'Done: {0} is paired with this device’s wireless debugging.': '完成：{0} 已与本机的无线调试配对。',
  'Connect to {0}': '连接到 {0}',
  'Open TwinDesk on {0}, then tap Connect on Overview.': '请在 {0} 上打开 TwinDesk，然后在「概览」中点「连接」。',
  'Developer options › USB debugging (Security settings): on, for touch, mouse and keyboard from {0}. It asks for a Mi account.': '开发者选项 › USB 调试（安全设置）：需开启，才能从 {0} 使用触摸、鼠标和键盘。该选项需要登录小米账号。',
  'Pair with {0}': '与 {0} 配对',
  'only the photos I choose': '仅我选择的照片',
  Connection: '连接',
  '{0} is sharing its screen': '{0} 正在共享屏幕',
  '{0} wants to pair': '{0} 请求配对',
  'Switch on the hotspot for {0}': '为 {0} 开启热点',
  '{0} isn’t on this phone’s network. Tap to open the hotspot, then type its name and password on {0}.': '{0} 不在这台手机的网络中。点按打开热点，然后在 {0} 上输入热点名称和密码。',
  'Switch on Wi-Fi for {0}': '为 {0} 开启 Wi-Fi',
  'To use {0}’s mouse and keyboard here without wireless debugging, switch on TwinDesk in Accessibility (Installed apps). Tap to open it.': '要在没有无线调试的情况下在此使用 {0} 的鼠标和键盘，请在辅助功能（已安装的应用）中开启 TwinDesk。点按打开。',
  'This build of TwinDesk can’t take {0}’s mouse and keyboard without wireless debugging. Set up wireless debugging, then {0} installs TwinDesk with notifications, which can (its Phone page › Install).': '此版本的 TwinDesk 无法在没有无线调试的情况下接收 {0} 的鼠标和键盘。请先配置无线调试，然后 {0} 会安装带通知功能的 TwinDesk（其「手机」页 ›「安装」），即可使用。',
  'Play this phone’s sound on {0}': '在 {0} 上播放这台手机的声音',
  'This phone’s sound plays on {0}': '这台手机的声音会在 {0} 上播放',
  'Display over other apps': '显示在其他应用上层',
  'TwinDesk {0} is available': 'TwinDesk {0} 已发布',
  'You have {0}. Tap to download the new version.': '当前版本为 {0}。点按下载新版本。',
  '{0} needs it for this screen, its mouse and keyboard here, and sound. Tap, then turn on USB debugging and Wireless debugging (both).': '{0} 需要它来显示这块屏幕、在此使用鼠标键盘并传输声音。点按后请同时开启 USB 调试和无线调试。',
  'Settings shows a 6-digit code. Type it here to pair with {0}.': '设置中会显示 6 位配对码。在此输入即可与 {0} 配对。',
  'Settings shows a 6-digit code under “Pair with device”. Tap Enter code, type it, and send. {0} does the rest.': '设置中的「使用配对码配对设备」会显示 6 位配对码。点「输入配对码」，输入后发送。剩下的由 {0} 完成。',
  'Copied on {0}': '在 {0} 上复制',
  '{0} · {1}': '{0} · {1}',
  'Screenshots go to': '截图发送到',
  Screenshots: '截图',
  'Send to computer': '发送到电脑',
  'Copied for {0}: paste there with Ctrl+Alt+V': '已为 {0} 复制：在那边按 Ctrl+Alt+V 粘贴',
  'Clipboard sent to {0}': '剪贴板已发送到 {0}',
  '{0} from {1}': '{0} 来自 {1}',
  'Not everything arrived from {0}': '来自 {0} 的内容未全部到达',
  '{0} is incomplete': '{0} 不完整',
  'Play this device’s sound': '播放本机的声音',
  'Not everything arrived from {0}.': '来自 {0} 的内容未全部到达。',
  '{0} is already there': '{0} 已存在',
  'Couldn’t make {0}': '无法创建 {0}',
  Storage: '存储',
  Computer: '电脑',
  'adb authorization timeout': 'ADB 授权超时',
};

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out); else if (e.name.endsWith('.kt')) out.push(p);
  }
  return out;
}
const decode = (raw) => raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
const encode = (s) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
let replaced = 0, glyph = 0;
for (const f of walk(SRC)) {
  let s = fs.readFileSync(f, 'utf8');
  const before = s;
  // 特殊键位符号 -> ASCII（安卓字体缺字形会显示成乱码）
  if (s.includes('⌃') || s.includes('⌥') || s.includes('⌘')) {
    s = s.replace(/⌃⌥/g, 'Ctrl+Alt+').replace(/⌘/g, 'Cmd+').replace(/⌃/g, 'Ctrl+').replace(/⌥/g, 'Alt+');
    glyph++;
  }
  s = s.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
    const text = decode(inner);
    if (!text.includes('$')) {
      if (Object.prototype.hasOwnProperty.call(T, text)) { replaced++; return '"' + encode(T[text]) + '"'; }
      return whole;
    }
    const idx = new Map(); const toks = [];
    const norm = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (tk) => {
      if (!idx.has(tk)) { idx.set(tk, toks.length); toks.push(tk); }
      return '{' + idx.get(tk) + '}';
    });
    if (Object.prototype.hasOwnProperty.call(T, norm)) {
      replaced++;
      return '"' + encode(T[norm].replace(/\{(\d+)\}/g, (_, i) => toks[+i] || '')) + '"';
    }
    return whole;
  });
  if (s !== before) fs.writeFileSync(f, s, 'utf8');
}
console.log('replaced:', replaced, '| files with glyph fix:', glyph);