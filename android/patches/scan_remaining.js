// 扫描当前源码中所有"像句子/标签"的英文残留
const fs = require('fs');
const path = require('path');
const DIR = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android';
const UI = ['MainActivity.kt', 'Setup.kt', 'Guide.kt', 'Ui.kt', 'Hub.kt', 'Notify.kt', 'Screenshots.kt', 'ShareActivity.kt',
  'ViewerActivity.kt', 'Remote.kt', 'Files.kt', 'Messages.kt', 'Calls.kt', 'Photos.kt', 'Sound.kt', 'PhoneSound.kt',
  'Clip.kt', 'FileClip.kt', 'FsBrowse.kt', 'FolderShare.kt', 'Updates.kt', 'Pointer.kt', 'Services.kt', 'PhoneInfo.kt', 'BtPad.kt', 'Input.kt'];
const decode = (raw) => raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\').replace(/\\\$/g, '$');
const isHuman = (t) => {
  if (!/[A-Za-z]{2}/.test(t)) return false;
  if (/[\u4e00-\u9fa5]/.test(t)) return false;
  if (/^[a-z0-9_.:\/-]+$/.test(t)) return false;
  if (/^(L[a-z]|java\.|android\.|androidx\.|kotlin\.|org\.$|com\.)/.test(t)) return false;
  if (/[{}<>;=\\]/.test(t)) return false;
  if (/(->|::|\bval\b|\bfun\b|\bclass\b|\bimport\b|\bwhen\b)/.test(t)) return false;
  if (/\$[A-Za-z]/.test(t) && !/\s/.test(t)) return false;
  return /\s/.test(t.trim()) || /^[A-Z][A-Za-z’']{2,}$/.test(t.trim());
};
const found = new Map();
for (const f of UI) {
  const p = path.join(DIR, f);
  if (!fs.existsSync(p)) continue;
  const s = fs.readFileSync(p, 'utf8');
  for (const m of s.matchAll(/"((?:\\.|[^"\\])*)"/g)) {
    const t = decode(m[1]);
    if (!isHuman(t)) continue;
    if (!found.has(t)) found.set(t, new Set());
    found.get(t).add(f);
  }
}
const arr = [...found.entries()];
console.log('可能残留的英文文案:', arr.length);
arr.forEach(([t, fs_]) => console.log(' -', JSON.stringify(t), '|', [...fs_].join(',')));
fs.writeFileSync('F:\\deep\\_twindesk_i18n\\build\\remaining_en.json', JSON.stringify(arr.map(([t, s]) => [t, [...s]]), null, 1), 'utf8');