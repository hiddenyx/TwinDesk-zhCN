/* TwinDesk 简体中文语言包 —— 主进程钩子（托盘菜单 / 原生对话框 / 通知 / 窗口标题）
 * 由 windows/main.js 与 windows/mesh/main.js 在顶部 require 引入；
 * 通过包装 Electron API 在运行时翻译，不改动应用自身业务逻辑。
 */
'use strict';

var electron = require('electron');
var fs = require('fs');
var path = require('path');

function loadDict() {
  var file = path.join(__dirname, 'zh-dict-main.json');
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch (e) { return { exact: {}, patterns: [] }; }
}

var DICT = loadDict();
var EXACT = DICT.exact || {};
var PATTERNS = (DICT.patterns || []).map(function (p) {
  var groups = [];
  // 必须先摘出占位符再转义，否则 \{0\} 会导致正则永远匹配不上
  var body = String(p[0]).replace(/\{(\d+)\}/g, function (_, n) {
    groups.push(parseInt(n, 10));
    return '\u0001' + n + '\u0001';
  });
  body = body.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\u0001(\d+)\u0001/g, '(.+?)');
  var re = null;
  try { re = new RegExp('^' + body + '$'); } catch (e) { return null; }
  return { re: re, zh: p[1], groups: groups };
}).filter(Boolean);

function fill(zh, groups, m) {
  return zh.replace(/\{(\d+)\}/g, function (_, n) {
    var idx = groups.indexOf(parseInt(n, 10));
    if (idx < 0 || m[idx + 1] == null) return '';
    var g = m[idx + 1];
    // 捕获组多为设备名/回退名（"pc" / "the other computer" 等）：用整句词典再翻一层
    if (g) g = tr(g);
    return g;
  });
}

function tr(s) {
  if (typeof s !== 'string' || !s) return s;
  if (Object.prototype.hasOwnProperty.call(EXACT, s)) return EXACT[s];
  if (s.length > 500 || !/[A-Za-z]{2}/.test(s)) return s;
  for (var i = 0; i < PATTERNS.length; i++) {
    var m = s.match(PATTERNS[i].re);
    if (m) return fill(PATTERNS[i].zh, PATTERNS[i].groups, m);
  }
  return s;
}

// ---- 菜单 ----
function translateTemplate(template) {
  if (!Array.isArray(template)) return template;
  return template.map(function (item) {
    if (!item || typeof item !== 'object') return item;
    var out = Object.assign({}, item);
    if (typeof out.label === 'string') out.label = tr(out.label);
    if (typeof out.toolTip === 'string') out.toolTip = tr(out.toolTip);
    if (out.submenu) out.submenu = translateTemplate(out.submenu);
    return out;
  });
}

try {
  var _buildFromTemplate = electron.Menu.buildFromTemplate;
  electron.Menu.buildFromTemplate = function (template) {
    return _buildFromTemplate.call(electron.Menu, translateTemplate(template));
  };
} catch (e) {}

// ---- 托盘提示 ----
try {
  var _setToolTip = electron.Tray.prototype.setToolTip;
  electron.Tray.prototype.setToolTip = function (tip) {
    return _setToolTip.call(this, tr(tip));
  };
} catch (e) {}

// ---- 窗口标题 ----
try {
  var _setTitle = electron.BrowserWindow.prototype.setTitle;
  electron.BrowserWindow.prototype.setTitle = function (title) {
    return _setTitle.call(this, tr(title));
  };
} catch (e) {}

// ---- 原生对话框 ----
function trBox(opts) {
  if (!opts || typeof opts !== 'object') return opts;
  var o = Object.assign({}, opts);
  if (typeof o.title === 'string') o.title = tr(o.title);
  if (typeof o.message === 'string') o.message = tr(o.message);
  if (typeof o.detail === 'string') o.detail = tr(o.detail);
  if (typeof o.buttonLabel === 'string') o.buttonLabel = tr(o.buttonLabel);
  if (Array.isArray(o.buttons)) o.buttons = o.buttons.map(tr);
  return o;
}
['showMessageBox', 'showMessageBoxSync', 'showOpenDialog', 'showOpenDialogSync', 'showSaveDialog', 'showSaveDialogSync'].forEach(function (name) {
  try {
    var orig = electron.dialog[name];
    if (typeof orig !== 'function') return;
    electron.dialog[name] = function (win, opts) {
      if (opts === undefined && win && typeof win === 'object' && !(win instanceof electron.BrowserWindow)) {
        return orig.call(electron.dialog, trBox(win));
      }
      return orig.call(electron.dialog, win, trBox(opts));
    };
  } catch (e) {}
});
try {
  var _errBox = electron.dialog.showErrorBox;
  electron.dialog.showErrorBox = function (title, content) {
    return _errBox.call(electron.dialog, tr(title), tr(content));
  };
} catch (e) {}

// ---- 系统通知 ----
try {
  var OrigNotification = electron.Notification;
  if (typeof OrigNotification === 'function') {
    function ZhNotification(opts) {
      var o = Object.assign({}, opts || {});
      if (typeof o.title === 'string') o.title = tr(o.title);
      if (typeof o.body === 'string') o.body = tr(o.body);
      if (typeof o.subtitle === 'string') o.subtitle = tr(o.subtitle);
      return new OrigNotification(o);
    }
    ZhNotification.isSupported = OrigNotification.isSupported
      ? OrigNotification.isSupported.bind(OrigNotification) : function () { return false; };
    ZhNotification.prototype = OrigNotification.prototype;
    electron.Notification = ZhNotification;
  }
} catch (e) {}

module.exports = { tr: tr, translateTemplate: translateTemplate };