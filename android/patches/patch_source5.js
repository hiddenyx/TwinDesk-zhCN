// 第五轮（只修 2 句，不改装到手机）：文件页两句拼接句
const fs = require('fs');
const path = require('path');
const DIR = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android\\MainActivity.kt';
const PAIRS = [
  ['They land in {0}’s Downloads. From any app: Share › {0}', '文件会保存到 {0} 的「下载」中。在任意应用中选择：分享 › {0}'],
  ['Paste them on {0} with ⌃⌥V, where you want them', '在 {0} 上按 ⌃⌥V，粘贴到想要的位置'],
];
const decode = (raw) => raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
const encode = (s) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
const key = ([t]) => t;
const map = new Map(PAIRS);
let n = 0;
const before = fs.readFileSync(DIR, 'utf8');
const after = before.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
  const text = decode(inner);
  if (!text.includes('$')) return whole;
  // 同一变量重复出现 -> 同一个占位号
  const idx = new Map(); const tokens = [];
  const norm = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (t) => {
    if (!idx.has(t)) { idx.set(t, tokens.length); tokens.push(t); }
    return '{' + idx.get(t) + '}';
  });
  const zh = map.get(norm);
  if (!zh) return whole;
  n++;
  return '"' + encode(zh.replace(/\{(\d+)\}/g, (_, i) => tokens[+i] || '')) + '"';
});
if (n) fs.writeFileSync(DIR, after, 'utf8');
console.log('replaced:', n);