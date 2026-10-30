// 第四轮：补回 Tab 标签等（前一轮 revert 后遗漏）+ 蓝牙连接说明模板
const fs = require('fs');
const path = require('path');
const SRC = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java';
const EXACT = {
  Open: '打开', Phone: '手机', Send: '发送', Calls: '通话', Messages: '短信', Camera: '摄像头',
  Off: '关闭', Home: '主屏', Pictures: '图片', Photos: '照片', General: '常规', Mode: '模式',
  Touchpad: '触控板', Tap: '点按', Hold: '长按', Scroll: '滚动', Smooth: '流畅', Now: '当前',
  Recent: '最近', Paired: '已配对', Download: '下载', Overview: '概览', Display: '显示器',
  About: '关于', Switch: '切换', Connect: '连接', OK: '确定', Sound: '声音',
  Clipboard: '剪贴板', Remote: '远程', Through: '方式', Accessibility: '辅助功能', Input: '输入',
  'Choose…': '选择…', Folder: '文件夹',
};
const TEMPLATES = [
  ['Connects to {0} over Bluetooth when it isn’t on this network. Files and screens move to a direct Wi-Fi link for a while', '当 {0} 不在同一网络时通过蓝牙连接。文件和屏幕会临时切换到 Wi-Fi 直连'],
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
for (const f of walk(SRC)) {
  const before = fs.readFileSync(f, 'utf8');
  let n = 0;
  const after = before.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
    const text = decode(inner);
    if (Object.prototype.hasOwnProperty.call(EXACT, text)) { n++; replaced++; return '"' + encode(EXACT[text]) + '"'; }
    if (text.includes('$')) {
      const tokens = [];
      const norm = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (t) => { tokens.push(t); return '{' + (tokens.length - 1) + '}'; });
      const zh = tmap.get(norm);
      if (zh) { n++; replaced++; return '"' + encode(zh.replace(/\{(\d+)\}/g, (_, i) => tokens[+i] || '')) + '"'; }
    }
    return whole;
  });
  if (after !== before) { filesChanged++; fs.writeFileSync(f, after, 'utf8'); }
}
console.log('files changed:', filesChanged, 'replaced:', replaced);