// 第二轮：扫描 Android Kotlin 源码里仍是英文的界面文案
const fs = require('fs');
const path = require('path');
const DIR = 'F:\\deep\\_twindesk_i18n\\src\\android\\app\\src\\main\\java\\com\\twindesk\\android';
// 只扫界面相关文件（排除协议/日志为主的文件）
const UI_FILES = ['MainActivity.kt', 'Setup.kt', 'Guide.kt', 'Ui.kt', 'Hub.kt', 'Notify.kt', 'Screenshots.kt',
  'ShareActivity.kt', 'ViewerActivity.kt', 'Remote.kt', 'Files.kt', 'Messages.kt', 'Calls.kt', 'Photos.kt',
  'Sound.kt', 'PhoneSound.kt', 'Clip.kt', 'FileClip.kt', 'FsBrowse.kt', 'FolderShare.kt', 'Updates.kt',
  'Pointer.kt', 'Services.kt', 'PhoneInfo.kt', 'BtPad.kt', 'Input.kt', 'Screens.kt', 'Photos.kt'];

const isHuman = (t) => {
  if (!/[A-Za-z]{2}/.test(t)) return false;
  if (/[\u4e00-\u9fa5]/.test(t)) return false;
  if (/^[a-z0-9_.:\/-]+$/.test(t)) return false;
  if (/^(L[a-z]|java\.|android\.|androidx\.|kotlin\.|org\.|com\.)/.test(t)) return false;
  if (/[{}<>;=$\\]/.test(t)) return false;
  if (/(->|::|\bval\b|\bfun\b|\bclass\b)/.test(t)) return false;
  return true;
};
const found = new Map();
for (const f of UI_FILES) {
  const p = path.join(DIR, f);
  if (!fs.existsSync(p)) continue;
  const s = fs.readFileSync(p, 'utf8');
  const re = /"((?:\\.|[^"\\])*)"/g;
  let m;
  while ((m = re.exec(s))) {
    const raw = m[1];
    const text = raw.replace(/\\u([0-9a-fA-F]{4})/g, (_, h) => String.fromCharCode(parseInt(h, 16))).replace(/\\"/g, '"').replace(/\\n/g, '\\n').replace(/\\\\/g, '\\');
    if (!isHuman(text)) continue;
    const key = text;
    if (!found.has(key)) found.set(key, { text, file: f, count: 1 });
    else found.get(key).count++;
  }
}
const arr = [...found.values()].sort((a, b) => b.count - a.count);
console.log('剩余的英文界面文案条目:', arr.length);
arr.slice(0, 140).forEach((x) => console.log(String(x.count).padStart(2), '|', JSON.stringify(x.text).slice(0, 110), '|', x.file));
fs.writeFileSync('F:\\deep\\_twindesk_i18n\\build\\android_leftovers.json', JSON.stringify(arr, null, 1), 'utf8');