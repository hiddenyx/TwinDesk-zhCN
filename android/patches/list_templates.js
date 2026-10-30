// 列出 Android Kotlin 源码里含 ${...} 模板且含英文的字符串
const fs = require('fs');
const path = require('path');
const DIR = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android';
const files = fs.readdirSync(DIR).filter((f) => f.endsWith('.kt'));
const out = new Map();
for (const f of files) {
  const s = fs.readFileSync(path.join(DIR, f), 'utf8');
  const re = /"((?:\\.|[^"\\])*)"/g;
  let m;
  while ((m = re.exec(s))) {
    const raw = m[1];
    if (!raw.includes('$')) continue;
    const text = raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\\\/g, '\\');
    if (!/[A-Za-z]{3,}/.test(text)) continue;
    if (/^[A-Za-z_][A-Za-z0-9_.]*$/.test(text)) continue;
    if (!out.has(text)) out.set(text, f);
  }
}
console.log('template strings w/ english:', out.size);
[...out.entries()].forEach(([t, f]) => console.log(f, '::', JSON.stringify(t)));
fs.writeFileSync('F:\\deep\\_twindesk_i18n\\build\\android_templates.json', JSON.stringify([...out.entries()], null, 1), 'utf8');