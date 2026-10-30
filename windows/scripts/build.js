// Merge teammate translations + manual overrides into runnable dictionaries.
const fs = require('fs');
const path = require('path');

const ROOT = 'F:\\deep\\_twindesk_i18n';
const PATCH = path.join(ROOT, 'patch', 'zh-cn');
fs.mkdirSync(PATCH, { recursive: true });

function readJson(p, fallback) {
  try { return JSON.parse(fs.readFileSync(p, 'utf8')); } catch (e) { return fallback; }
}

const transDir = path.join(ROOT, 'trans');
const files = fs.existsSync(transDir) ? fs.readdirSync(transDir).filter((f) => /^batch\d+\.json$/.test(f)) : [];
const dict = new Map();
const problems = [];
for (const f of files) {
  const obj = readJson(path.join(transDir, f), null);
  if (!obj) { problems.push(`${f}: not valid JSON or missing`); continue; }
  for (const [en, zh] of Object.entries(obj)) {
    if (typeof zh !== 'string' || !zh.trim()) { problems.push(`${f}: empty translation for "${en}"`); continue; }
    dict.set(en, zh);
  }
}

// placeholder integrity check
const ph = (s) => (s.match(/\{\d+\}|%[sd]/g) || []).sort().join(',');
for (const [en, zh] of dict) {
  const a = ph(en), b = ph(zh);
  if (a !== b) problems.push(`placeholder mismatch: "${en}" (${a}) -> "${zh}" (${b})`);
}

const manual = readJson(path.join(ROOT, 'manual.json'), { exact: {}, patterns: [] });
for (const [en, zh] of Object.entries(manual.exact || {})) dict.set(en, zh);

const exact = {};
const patterns = [];
for (const [en, zh] of dict) {
  if (zh === en) continue;
  if (/\{\d+\}/.test(en)) patterns.push([en, zh]);
  else exact[en] = zh;
}
for (const p of manual.patterns || []) patterns.push(p);

// longest first so specific patterns win
patterns.sort((a, b) => b[0].length - a[0].length);

const header = '/* TwinDesk zh-CN 语言包数据（自动生成，勿手改；源：' + (files.length ? files.join(',') : 'none') + '） */\n';
fs.writeFileSync(path.join(PATCH, 'zh-dict.js'),
  header + 'window.__TD_ZH = ' + JSON.stringify({ exact, patterns }) + ';\n', 'utf8');
fs.writeFileSync(path.join(PATCH, 'zh-dict-main.json'),
  JSON.stringify({ exact, patterns }, null, 0), 'utf8');

console.log(`batches merged: ${files.length}`);
console.log(`exact: ${Object.keys(exact).length}, patterns: ${patterns.length}`);
console.log(`problems: ${problems.length}`);
problems.slice(0, 40).forEach((p) => console.log('  ! ' + p));
fs.writeFileSync(path.join(ROOT, 'build_report.txt'), problems.join('\n'), 'utf8');