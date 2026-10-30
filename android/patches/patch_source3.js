// 第三轮：补连接后页面（屏幕/光标/文件/更多）的英文句子与标签
const fs = require('fs');
const path = require('path');

const SRC = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java';
const EXACT = {
  'Choose…': '选择…',
  Remote: '远程',
  Clipboard: '剪贴板',
  Sound: '声音',
  Through: '方式',
  Accessibility: '辅助功能',
  Input: '输入',
  Folder: '文件夹',
  'Settings opens at Developer options: switch USB debugging and Wireless debugging on, then come back.':
    '设置将在「开发者选项」中打开：开启 USB 调试和无线调试，然后返回。',
  'Connecting over Bluetooth…': '正在通过蓝牙连接…',
  'Pair device with pairing code': '使用配对码配对设备',
  'Copy to computer': '复制到电脑',
  'Needs TwinDesk with notifications': '需要带通知功能的 TwinDesk',
};

const TEMPLATES = [
  ['Show {0}’s screen', '显示 {0} 的屏幕'],
  ['{0} isn’t sharing its screen.', '{0} 没有共享屏幕。'],
  ['Extend makes {0} a second screen the size of this one', '扩展会让 {0} 成为与本机同样大小的第二块屏幕'],
  ['Move {0}’s pointer off the edge of its screen to use this device.', '把 {0} 的指针移出其屏幕边缘即可使用这台设备。'],
  ['Push {0}’s pointer past its edge to come here. Double-tap {1} to take everything back.', '把 {0} 的指针推过边缘即可来到本机。双击 {1} 取回控制权。'],
  ['Send to {0}', '发送到 {0}'],
  ['They land in {0}’s Downloads. From any app: Share › {0}', '文件会保存到 {0} 的「下载」中。在任意应用中选择：分享 › {0}'],
  ['Paste them on {0} with {1}, where you want them', '在 {0} 上按 {1}，粘贴到想要的位置'],
  ['Drop to send to {0}', '松开即发送到 {0}'],
  ['Drag files here to send them to {0}', '把文件拖到这里发送到 {0}'],
  ['Where {0} is', '{0} 在哪个方向'],
  ['Off while you’re on the same Wi-Fi as {0}, on again if it drops', '与 {0} 在同一 Wi-Fi 时关闭，断开后自动恢复'],
  ['Use this device as a screen for {0}: its own screen (Mirror), or a second one (Extend).', '把本机用作 {0} 的屏幕：镜像它的屏幕，或作为第二块屏幕（扩展）。'],
  ['This screen on {0}', '这块屏幕显示到 {0}'],
  ['Show this screen on {0}', '在 {0} 上显示这块屏幕'],
  ['Use {0}’s mouse and keyboard on {1}', '在 {1} 上使用 {0} 的鼠标和键盘'],
  ['From {0}', '来自 {0}'],
  ['To {0}', '发送到 {0}'],
  ['{0} needs TwinDesk with notifications: Android doesn’t let downloaded apps read texts. {1} installs it once wireless debugging is set up (its Phone page › Install). Your settings and pairing stay.', '{0} 需要带通知功能的 TwinDesk：Android 不允许下载安装的应用读取短信。无线调试配置好后 {1} 会安装它（其「手机」页 › 安装）。你的设置和配对都会保留。'],
  // 第二轮已经加过、这里保留（幂等）
  ['Files arriving, invites from {0}, and TwinDesk staying connected', '文件接收、来自 {0} 的邀请，以及 TwinDesk 保持连接'],
  ['Answer and make calls on {0}', '在 {0} 上接听和拨打电话'],
  ['Read and send texts on {0}', '在 {0} 上读取和发送短信'],
  ['Your photos and videos on {0}', '在 {0} 上查看照片和视频'],
  ['This device’s storage and shared folders on {0}, no ADB', '在 {0} 上访问本机存储和共享文件夹，无需 ADB'],
  ['So {0} can reach this device any time', '这样 {0} 可随时连接这台设备'],
  ['To show this device’s notifications on {0}', '在 {0} 上显示本机的通知'],
  ['Needs TwinDesk with notifications ({0} installs it)', '需要带通知功能的 TwinDesk（{0} 会安装它）'],
  ['Some access is missing: {0}', '部分权限未授予：{0}'],
  ['Comes with TwinDesk with notifications, which {0} installs (its Phone page)', '由带通知功能的 TwinDesk 提供，{0} 会安装（其「手机」页）'],
  ['Not connected. Connect to {0} on the Overview page.', '未连接。请在「概览」页连接到 {0}。'],
  ['Browse this phone’s storage on {0}, no ADB', '在 {0} 上浏览这台手机的存储，无需 ADB'],
  ['Allow All files access, so {0} can browse this phone’s storage. TwinDesk reads and writes the files itself: no ADB.', '允许「所有文件访问」，让 {0} 能浏览这台手机的存储。TwinDesk 自己读写文件：无需 ADB。'],
  ['Answer, make and end this phone’s calls on {0}.', '在 {0} 上接听、拨打和挂断这台手机的通话。'],
  ['Read and send this phone’s texts on {0}.', '在 {0} 上读取和发送这台手机的短信。'],
  ['See this phone’s photos and videos on {0} and copy them there.', '在 {0} 上查看这台手机的照片和视频，并复制过去。'],
  ['Browse this phone’s storage on {0}, and copy files both ways.', '在 {0} 上浏览这台手机的存储，并双向复制文件。'],
  ['This device’s camera on {0}: in TwinDesk there, and as a camera its apps can pick.', '本机摄像头投到 {0}：在那边 TwinDesk 中，也可作为其应用可选的摄像头。'],
  ['This device’s windows are open on {0}. Close shuts them', '本机的窗口已在 {0} 上打开。关闭即结束'],
];

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.kt')) out.push(p);
  }
  return out;
}
function decode(raw) {
  return raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
}
// 注意：不转义 $，否则 Kotlin 模板会被破坏
function encode(s) {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

const tmap = new Map(TEMPLATES);
let filesChanged = 0, replaced = 0;
const log = [];
for (const f of walk(SRC)) {
  const before = fs.readFileSync(f, 'utf8');
  let count = 0;
  const after = before.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
    const text = decode(inner);
    if (Object.prototype.hasOwnProperty.call(EXACT, text)) {
      count++; replaced++;
      return '"' + encode(EXACT[text]) + '"';
    }
    if (text.includes('$')) {
      const tokens = [];
      const normalized = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (t) => { tokens.push(t); return '{' + (tokens.length - 1) + '}'; });
      const zh = tmap.get(normalized);
      if (zh) {
        const out = zh.replace(/\{(\d+)\}/g, (_, i) => tokens[Number(i)] || '');
        count++; replaced++;
        log.push(`${path.basename(f)}: ${JSON.stringify(text)} -> ${JSON.stringify(out)}`);
        return '"' + encode(out) + '"';
      }
    }
    return whole;
  });
  if (after !== before) { filesChanged++; fs.writeFileSync(f, after, 'utf8'); }
  if (count) log.push(`-- ${path.basename(f)}: ${count} 处`);
}
console.log(`files changed: ${filesChanged}, replaced: ${replaced}`);
fs.writeFileSync('F:\\deep\\_twindesk_i18n\\build\\source_zh3_log.txt', log.join('\n'), 'utf8');
log.filter((l) => l.includes('->')).slice(0, 15).forEach((l) => console.log(' ', l));