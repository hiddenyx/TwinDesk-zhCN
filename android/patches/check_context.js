// 检查候选英文标签是否被用于逻辑比较（==/!=/when 分支/map key），避免翻译破坏逻辑
const fs = require('fs');
const path = require('path');
const DIR = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android';
const CANDIDATES = ['Open', 'Phone', 'Sound', 'Clipboard', 'Send', 'Calls', 'Messages', 'Input', 'Camera', 'Off',
  'Home', 'Pictures', 'Photos', 'Remote', 'General', 'Mode', 'Touchpad', 'Tap', 'Hold', 'Scroll', 'Smooth',
  'Now', 'Recent', 'Paired', 'Download', 'Overview', 'Display', 'About', 'Switch', 'Connect', 'Wi-Fi Direct', 'OK',
  'Connector', 'Files', 'Screen', 'Cursor', 'More', 'Play', 'Pause', 'Stop', 'Back', 'Start'];
const files = fs.readdirSync(DIR).filter((f) => f.endsWith('.kt'));
const report = {};
for (const c of CANDIDATES) report[c] = { eq: 0, whenBranch: 0, keyLike: 0, plain: 0, files: new Set() };
for (const f of files) {
  const lines = fs.readFileSync(path.join(DIR, f), 'utf8').split(/\r?\n/);
  for (const line of lines) {
    for (const c of CANDIDATES) {
      const q = '"' + c + '"';
      if (!line.includes(q)) continue;
      report[c].files.add(f);
      if (new RegExp('(==|!=)\\s*' + q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).test(line)) report[c].eq++;
      else if (new RegExp(q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '\\s*->').test(line)) report[c].whenBranch++;
      else if (/put\(|getString\(|optString\(|get\("|set\("|key|Key/.test(line)) report[c].keyLike++;
      else report[c].plain++;
    }
  }
}
const safe = [], risky = [];
for (const c of CANDIDATES) {
  const r = report[c];
  const line = `${JSON.stringify(c)} plain=${r.plain} eq=${r.eq} when=${r.whenBranch} key=${r.keyLike} files=${[...r.files].join(',')}`;
  if (r.eq || r.whenBranch || r.keyLike) risky.push(line); else safe.push(line);
}
console.log('=== 安全（纯展示，可直接翻译）===');
safe.forEach((l) => console.log(' ', l));
console.log('=== 有风险（参与逻辑，先别动）===');
risky.forEach((l) => console.log(' ', l));