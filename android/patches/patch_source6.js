// 第六轮：屏幕页（手写笔/播放）、光标页（蓝牙）、文件页、概览页剩余英文
const fs = require('fs');
const path = require('path');
const SRC = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java';
const EXACT = {
  Stylus: '手写笔',
  Pen: '笔',
  Playback: '播放',
  'Over Bluetooth': '通过蓝牙',
  Where: '位置',
};
const TEMPLATES = [
  ['See and save them on {0}', '在 {0} 上查看并保存'],
  ['Pressure, tilt, the eraser and the side button, as a drawing tablet on {0}; your hand on the screen does nothing while the pen is near. Off: the stylus is a mouse', '压力、倾斜、橡皮擦和侧键，像在 {0} 上使用绘图板；笔靠近时手部触控不生效。关闭：手写笔仅当作鼠标。'],
  ['{0}’s sound with the screen', '把 {0} 的声音随屏幕一起传输'],
  ['When {0} shares sound with its screen', '当 {0} 随屏幕共享声音时'],
  ['When {0} isn’t on this network and you’re connected through Bluetooth (slower). Off: nothing crosses until you’re on Wi-Fi again. The same on both', '当 {0} 不在本网络、你通过蓝牙连接时（较慢）。关闭：在重新连上 Wi-Fi 前不传输任何内容。两边一致'],
  ['Files dropped on this device’s window on {0} come here', '拖到 {0} 上本机窗口的文件会保存到这里'],
  ['Off. Switch it on here or on {0}.', '已关闭。请在这里或 {0} 上开启。'],
  ['On USB, Wi-Fi or Wi-Fi Direct. Over Bluetooth {0} says so, with Connect with Wi-Fi Direct', '通过 USB、Wi-Fi 或 Wi-Fi Direct 连接。走蓝牙时 {0} 会提示，并给出「用 Wi-Fi Direct 连接」的选项'],
  ['ADB (wireless debugging): full control. Over Bluetooth without it, Accessibility', 'ADB（无线调试）：完全控制。走蓝牙且未开无线调试时，使用辅助功能'],
];
function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out); else if (e.name.endsWith('.kt')) out.push(p);
  }
  return out;
}
const decode = (raw) => raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
const encode = (s) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
const tmap = new Map(TEMPLATES);
let replaced = 0, filesChanged = 0;
const log = [];
for (const f of walk(SRC)) {
  const before = fs.readFileSync(f, 'utf8');
  let n = 0;
  const after = before.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
    const text = decode(inner);
    if (Object.prototype.hasOwnProperty.call(EXACT, text)) { n++; replaced++; return '"' + encode(EXACT[text]) + '"'; }
    if (text.includes('$')) {
      const idx = new Map(); const tokens = [];
      const norm = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (t) => {
        if (!idx.has(t)) { idx.set(t, tokens.length); tokens.push(t); }
        return '{' + idx.get(t) + '}';
      });
      const zh = tmap.get(norm);
      if (zh) { n++; replaced++; const out = zh.replace(/\{(\d+)\}/g, (_, i) => tokens[+i] || ''); log.push(`${path.basename(f)}: ${JSON.stringify(text)} -> ${JSON.stringify(out)}`); return '"' + encode(out) + '"'; }
    }
    return whole;
  });
  if (after !== before) { filesChanged++; fs.writeFileSync(f, after, 'utf8'); }
}
console.log('files changed:', filesChanged, 'replaced:', replaced);
log.slice(0, 12).forEach((l) => console.log(' ', l));