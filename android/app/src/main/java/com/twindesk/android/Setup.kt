// TwinDesk — Copyright (C) 2026 inukaasith. GPL-3.0, see LICENSE.
// The setup screen (like the Mac's): shown on start while something TwinDesk needs isn't allowed
// yet, one row per permission with its own Allow button. "暂时跳过" hides it until something
// else goes missing (the skipped set is remembered); General › Setup has the same rows.
package com.twindesk.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SetupCheck {
    class Item(val id: String, val title: String, val hint: String, val icon: ImageVector, val colors: List<Color>, val required: Boolean, val done: Boolean)

    fun items(ctx: Context, core: Core): List<Item> {
        val pm = ctx.getSystemService(PowerManager::class.java)
        val c = { a: Long, b: Long -> listOf(Color(a), Color(b)) }
        val list = mutableListOf<Item>()
        if (Build.VERSION.SDK_INT >= 33) list += Item(
            "notify", "通知", "接收电脑发来的文件、剪贴板内容和通知",
            Ico.bell, c(0xFFFF6961, 0xFFFF3B30), true,
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
        if (core.notifs.supported) list += Item("listener", "通知使用权", "在电脑上查看这台设备的通知", Ico.bell, c(0xFFFFB340, 0xFFFF9500), true, core.notifs.hasAccess)
        list += Item("battery", "在后台运行", "让电脑随时可以连接这台设备", Ico.general, c(0xFFAEAEB2, 0xFF636366), true, pm.isIgnoringBatteryOptimizations(ctx.packageName))
        list += Item("overlay", "显示指针", "电脑的鼠标在此操作时，其指针可显示在其他应用之上", Ico.cursor, c(0xFFFFB340, 0xFFFF9500), true, core.pointer.allowed)
        list += Item("adb", "无线调试", "在电脑上显示这块屏幕、在此使用它的鼠标和键盘以及声音。电脑只需配对一次", Ico.display, c(0xFF5AC8FA, 0xFF0A84FF), false, core.adb.wifiDebugging)
        if (Build.VERSION.SDK_INT >= 30) list += Item("wallpaper", "在电脑上显示壁纸", "可选：Android 只在授予「所有文件访问」后才共享壁纸", Ico.files, c(0xFF64D2FF, 0xFF0A84FF), false, android.os.Environment.isExternalStorageManager())
        return list
    }

    // What is missing now, as a key: the screen shows unless that exact set was skipped.
    fun missingKey(items: List<Item>) = items.filter { it.required && !it.done }.joinToString(",") { it.id }

    fun shouldShow(ctx: Context, core: Core): Boolean {
        val key = missingKey(items(ctx, core))
        return key.isNotEmpty() && key != core.store.prefsString("setup.skipped", "")
    }

    fun skip(ctx: Context, core: Core) {
        core.store.setPrefsString("setup.skipped", missingKey(items(ctx, core)))
        core.changed()
    }

    fun open(ctx: Context, core: Core, id: String, askNotify: () -> Unit) {
        val pkg = Uri.parse("package:${ctx.packageName}")
        when (id) {
            "notify" -> askNotify()
            "listener" -> ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            "battery" -> @Suppress("BatteryLife") ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
            "overlay" -> ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg))
            "adb" -> core.adb.turnOn()
            "wallpaper" -> ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg))
        }
    }
}

@Composable
fun SetupScreen(core: Core, ctx: Context, v: Int, done: () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") v
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { core.changed() }
    val items = SetupCheck.items(ctx, core)
    val ready = items.none { it.required && !it.done }
    Box(Modifier.fillMaxSize().background(pal.content).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 620.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Logo(84)
            Text("设置 TwinDesk", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.padding(top = 12.dp))
            Text(
                "允许以下权限，让这台设备与你的电脑协同如一。之后可随时在「通用 › 设置」中更改。",
                color = pal.muted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )
            MacCard {
                items.forEachIndexed { i, it ->
                    Column {
                        if (i > 0) Box(Modifier.padding(start = 58.dp).fillMaxWidth().height(1.dp).background(pal.line))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Tile(it.icon, it.colors, 30)
                            Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp)) {
                                Text(it.title + if (it.required) "" else "（可选）", color = pal.text, fontSize = 15.sp)
                                Text(it.hint, color = pal.muted, fontSize = 12.5.sp)
                            }
                            if (it.done) Pill("已允许", "good")
                            else Button(
                                onClick = { SetupCheck.open(ctx, core, it.id) { askNotify.launch(Manifest.permission.POST_NOTIFICATIONS) } },
                                colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White), shape = RoundedCornerShape(8.dp),
                            ) { Text(if (it.id == "adb") "开启" else "允许") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (ready) done() else SetupCheck.skip(ctx, core) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White),
            ) { Text(if (ready) "继续" else "暂时跳过", modifier = Modifier.padding(vertical = 4.dp)) }
            if (!ready) Text("不允许这些也能使用 TwinDesk，但部分功能将不可用。", color = pal.faint, fontSize = 12.5.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
