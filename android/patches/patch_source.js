// 从源码汉化：把 trans_table.json + fix2 的英→中映射应用到 Kotlin 源码字符串字面量
const fs = require('fs');
const path = require('path');

const ROOT = 'F:\\deep\\_twindesk_i18n';
const SRC = path.join(ROOT, 'src', 'android', 'app', 'src', 'main');
const TABLE = path.join(ROOT, 'android', 'trans_table.json');
const DRY = process.argv.includes('--dry');

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (/\.(kt|java|xml|html|js)$/i.test(e.name)) out.push(p);
  }
  return out;
}

// 构建映射
const t = JSON.parse(fs.readFileSync(TABLE, 'utf8'));
const map = new Map();
for (const e of t.global || []) if (e.en && e.zh && e.en !== e.zh) map.set(e.en, e.zh);
// 二轮修复（smali 阶段补的英文）
const extra = {
  'the computer': '电脑',
  Nearby: '附近',
  Move: '把',
  Notifications: '通知',
  'Wireless debugging': '无线调试',
  Allow: '允许',
  Turn: '开启',
  More: '更多',
  Screen: '屏幕',
  Cursor: '光标',
  Files: '文件',
};
// 只把 fix2 中“英文→中文”的加进来（忽略中文→中文的续接修复）
for (const [k, v] of Object.entries(extra)) if (!map.has(k)) map.set(k, v);

function decodeKotlin(raw) {
  return raw
    .replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16)))
    .replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\"/g, '"').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
}
function encodeKotlin(s) {
  return s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
}

let files = 0, repl = 0;
const log = [];
for (const f of walk(SRC)) {
  if (!/\.kt$/i.test(f)) continue; // 先只改 Kotlin（Compose 文案）
  const before = fs.readFileSync(f, 'utf8');
  const after = before.replace(/"((?:\\.|[^"\\])*)"/g, (whole, inner) => {
    const text = decodeKotlin(inner);
    const zh = map.get(text);
    if (!zh) return whole;
    repl++;
    log.push(`${path.relative(SRC, f)}: ${JSON.stringify(text)} -> ${JSON.stringify(zh)}`);
    return '"' + encodeKotlin(zh) + '"';
  });
  if (after !== before) {
    files++;
    if (!DRY) fs.writeFileSync(f, after, 'utf8');
  }
}
console.log(`files changed: ${files}, literals replaced: ${repl}${DRY ? ' (dry run)' : ''}`);
fs.writeFileSync(path.join(ROOT, 'build', 'source_zh_log.txt'), log.join('\n'), 'utf8');
const sample = log.slice(0, 12);
sample.forEach((l) => console.log('  ' + l));