/* TwinDesk 简体中文语言包 —— 渲染进程引擎（非官方补丁，随更新失效，可一键还原）
 * 依赖同目录 zh-dict.js 提供的 window.__TD_ZH = { exact: {...}, patterns: [...] }
 * 做法：不改动应用源码逻辑，只在运行时把界面上的英文文本节点/提示属性替换为中文。
 */
(function () {
  'use strict';
  var TD = window.__TD_ZH;
  if (!TD || TD.__installed) return;
  TD.__installed = true;

  var EXACT = TD.exact || {};

  // ---- 把 "… {0} … {1}" 这类模板编译成正则 ----
  // 注意：必须先摘出占位符再转义，否则 \{0\} 会导致正则永远匹配不上
  function escapeRe(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }
  var PATTERNS = (TD.patterns || []).map(function (p) {
    var src = p[0], zh = p[1], groups = [];
    var body = String(src).replace(/\{(\d+)\}/g, function (_, n) {
      groups.push(parseInt(n, 10));
      return '\u0001' + n + '\u0001';
    });
    body = escapeRe(body).replace(/\u0001(\d+)\u0001/g, '(.+?)');
    var re;
    try { re = new RegExp('^' + body + '$'); } catch (e) { return null; }
    return { re: re, zh: zh, groups: groups };
  }).filter(Boolean);

  function fill(zh, groups, m) {
    return zh.replace(/\{(\d+)\}/g, function (_, n) {
      var idx = groups.indexOf(parseInt(n, 10));
      if (idx < 0 || m[idx + 1] == null) return '';
      var g = m[idx + 1];
      // 捕获组多为设备名/回退名（"pc" / "the other computer" 等）：用整句词典再翻一层
      if (g) g = trCore(g);
      return g;
    });
  }

  function trCore(s) {
    if (!s) return s;
    if (Object.prototype.hasOwnProperty.call(EXACT, s)) return EXACT[s];
    if (s.length > 500 || !/[A-Za-z]{2}/.test(s)) return s;
    for (var i = 0; i < PATTERNS.length; i++) {
      var m = s.match(PATTERNS[i].re);
      if (m) return fill(PATTERNS[i].zh, PATTERNS[i].groups, m);
    }
    return s;
  }

  // 保留原有首尾空白
  function translateText(text) {
    if (!text) return text;
    var lead = /^\s*/.exec(text)[0];
    var trail = /\s*$/.exec(text)[0];
    var core = text.slice(lead.length, text.length - trail.length);
    if (!core) return text;
    var out = trCore(core.replace(/\s+/g, ' '));
    return out === core ? text : lead + out + trail;
  }

  var ATTRS = ['title', 'placeholder', 'aria-label', 'alt'];
  // SVG 不再整体跳过：<text> 里也有 UI 标签；INPUT/TEXTAREA 只跳过子节点遍历（属性照翻）
  var SKIP_TAGS = { SCRIPT: 1, STYLE: 1, NOSCRIPT: 1, TEXTAREA: 1, CODE: 1, PRE: 1, CANVAS: 1 };

  function translateAttrs(el) {
    for (var i = 0; i < ATTRS.length; i++) {
      var a = ATTRS[i];
      if (!el.hasAttribute || !el.hasAttribute(a)) continue;
      var v = el.getAttribute(a);
      if (!v || !/[A-Za-z]{2}/.test(v)) continue;
      var t = translateText(v.trim());
      if (t !== v) { el.setAttribute(a, t); }
    }
  }

  function translateNode(node) {
    if (node.nodeType === 3) {
      var v = node.nodeValue;
      if (!v || !/[A-Za-z]{2}/.test(v)) return;
      var t = translateText(v);
      if (t !== v) { node.nodeValue = t; }
      return;
    }
    if (node.nodeType !== 1) return;
    var tag = node.tagName ? node.tagName.toUpperCase() : '';
    if (node.isContentEditable) return;
    if (node.hasAttribute && (node.hasAttribute('data-no-i18n') || node.getAttribute('contenteditable') === 'true')) return;
    // 属性（placeholder/title/aria-label 等）对 INPUT/TEXTAREA 也要翻译，只跳过子节点遍历
    translateAttrs(node);
    if (SKIP_TAGS[tag]) return;
    var kids = node.childNodes;
    for (var i = 0; i < kids.length; i++) translateNode(kids[i]);
  }

  function translateTree(root) {
    if (!root) return;
    if (root.nodeType === 3) { translateNode(root); return; }
    translateNode(root);
  }

  function translateTitle() {
    var t = document.title;
    var n = translateText(t);
    if (n !== t) document.title = n;
  }

  // ---- 动态内容：监听 DOM 变化 ----
  function startObserver() {
    var mo = new MutationObserver(function (records) {
      for (var i = 0; i < records.length; i++) {
        var r = records[i];
        if (r.type === 'childList') {
          for (var j = 0; j < r.addedNodes.length; j++) translateNode(r.addedNodes[j]);
        } else if (r.type === 'characterData') {
          translateNode(r.target);
        } else if (r.type === 'attributes') {
          translateAttrs(r.target);
        }
      }
    });
    mo.observe(document.documentElement, {
      childList: true, subtree: true, characterData: true,
      attributes: true, attributeFilter: ATTRS,
    });
  }

  // ---- 原生弹窗 ----
  function wrapDialog(fnName) {
    var orig = window[fnName];
    if (typeof orig !== 'function') return;
    window[fnName] = function (msg) {
      var args = Array.prototype.slice.call(arguments);
      if (typeof args[0] === 'string') args[0] = trCore(args[0]);
      return orig.apply(window, args);
    };
  }

  function boot() {
    try { translateNode(document.documentElement); } catch (e) {}
    try { translateTitle(); } catch (e) {}
    try { startObserver(); } catch (e) {}
    wrapDialog('alert'); wrapDialog('confirm'); wrapDialog('prompt');
    // 标题/状态可能稍后才被脚本设置
    var n = 0;
    var timer = setInterval(function () {
      translateTitle();
      if (++n >= 20) clearInterval(timer);
    }, 500);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();