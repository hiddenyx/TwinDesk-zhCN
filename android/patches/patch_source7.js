// 第七轮：Slide 标签 + 触控板说明
const fs = require('fs');
const P = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android\\MainActivity.kt';
const EXACT = { Slide: '滑动' };
const TPL = [
  ['Slide to move {0}’s pointer, tap to click, hold to right click, two fingers to scroll. Keys and media keys too',
    '滑动可移动 {0} 的指针，点按即单击，长按即右键，双指滚动。也支持按键和媒体键'],
];
const decode = (raw) => raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
const encode = (s) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
const tmap = new Map(TPL);
let n = 0;
let s = fs.readFileSync(P, 'utf8');
s = s.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
  const text = decode(inner);
  if (Object.prototype.hasOwnProperty.call(EXACT, text)) { n++; return '"' + encode(EXACT[text]) + '"'; }
  if (text.includes('$')) {
    const idx = new Map(); const toks = [];
    const norm = text.replace(/\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*/g, (tk) => {
      if (!idx.has(tk)) { idx.set(tk, toks.length); toks.push(tk); }
      return '{' + idx.get(tk) + '}';
    });
    const zh = tmap.get(norm);
    if (zh) { n++; return '"' + encode(zh.replace(/\{(\d+)\}/g, (_, i) => toks[+i] || '')) + '"'; }
  }
  return whole;
});
fs.writeFileSync(P, s, 'utf8');
console.log('replaced:', n);