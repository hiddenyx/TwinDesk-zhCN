// TwinDesk — Copyright (C) 2026 inukaasith. GPL-3.0, see LICENSE.
// The share sheet's two targets, "发送到电脑" and "复制到电脑" (the activity alias),
// and the see-through activity that reads the clipboard for a moment (Android lets only the app in
// front read it) for the Quick Settings tile and the notification's "发送剪贴板".
package com.twindesk.android

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.IntentCompat

class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core = Core.get(this)
        val copy = intent.component?.className?.endsWith("CopyToComputer") == true
        val host = core.host
        if (host == null) {
            toast("未连接。打开 TwinDesk 以连接电脑。")
            return finish()
        }
        val uris = mutableListOf<Uri>()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris.add(it) }
                if (uris.isEmpty()) {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!text.isNullOrEmpty()) {
                        core.clip.sendText(text)
                        toast("在 ${host.name} 上复制")
                        return finish()
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { uris.addAll(it) }
        }
        if (uris.isEmpty()) return finish()
        // The share's read grant ends with this activity; the files are read later, when the
        // computer accepts them. Keep a grant of our own until the process ends.
        for (u in uris) runCatching { grantUriPermission(packageName, u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        // Reading names and sizes may take a moment for big shares; the URIs stay readable for us.
        core.bg.execute {
            val items = core.files.describe(uris)
            val ok = if (copy) core.fclip.copy(items) else core.files.send(items, "send")
            runOnUiThread {
                toast(
                    when {
                        !ok -> "Couldn’t send to ${host.name}"
                        copy -> "已为 ${host.name} 复制：在那边按 Ctrl+Alt+V 粘贴"
                        else -> "Sending ${Files.plural(items.size, "item")} to ${host.name}"
                    },
                )
            }
        }
        finish()
    }

    private fun toast(t: String) = Toast.makeText(applicationContext, t, Toast.LENGTH_SHORT).show()
}

class ClipActivity : Activity() {
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val core = Core.get(this)
        val cm = getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip
        val name = core.host?.name ?: "你的电脑"
        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString() else null
        val uri = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        val msg = when {
            core.host == null -> "未连接到电脑"
            uri != null -> {
                val items = core.files.describe((0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri })
                if (core.fclip.copy(items)) "已为 $name 复制：在那边按 Ctrl+Alt+V 粘贴" else "无法复制该内容"
            }
            text.isNullOrEmpty() -> "剪贴板为空"
            core.clip.sendText(text) -> "剪贴板已发送到 $name"
            else -> "无法发送剪贴板"
        }
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        finish()
    }
}

// The home screen shortcut "无线调试": on at once if the Mac granted it, else the setting.
class AdbActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core = Core.get(this)
        if (core.adb.enable(true, byPerson = true)) {
            core.adb.sendState()
            Toast.makeText(applicationContext, "无线调试已开启", Toast.LENGTH_SHORT).show()
        } else core.adb.openSettings()
        finish()
    }
}
