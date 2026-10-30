// TwinDesk — Copyright (C) 2026 inukaasith. GPL-3.0, see LICENSE.
// The app's window, laid out like TwinDesk on the Mac: a sidebar with the TwinDesk logo and the
// pages in the Mac's groups (Display, Input, Share, More, then General and About), each page a
// heading with its main switch over cards of rows. On a phone held upright the sidebar becomes
// tabs along the bottom (the rest under More). The building blocks are in Ui.kt.
package com.twindesk.android

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var resumed = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TwinService.start(this)
        setContent { Theme { App(Core.get(this), resumed.value) } }
    }

    override fun onResume() {
        super.onResume()
        shown = true
        resumed.value++ // permissions may have changed in Settings
        Core.get(this).adb.check() // back from Settings: is wireless debugging on now?
        Core.get(this).hub.let { if (it.callsOn || it.messagesOn || it.mediaOn || it.filesOn) it.permsChanged() }
        Core.get(this).share.let { if (it.on && it.serving != it.allowed) it.core.bg.execute { it.sync() } } // All files access just allowed (or taken away)
    }

    override fun onPause() {
        super.onPause()
        shown = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // In front: Android lets us read the clipboard now.
        if (hasFocus) Core.get(this).let { if (it.sync.get("clipboard", "out")) it.clip.sendCurrent(false) }
    }

    companion object {
        @Volatile var shown = false // on screen: Android lets us open a prompt (Hub.ask)
    }
}

private fun grad(a: Long, b: Long) = listOf(Color(a), Color(b))

// The pages, with the Mac's tile colours and groups.
private enum class Tab(val title: String, val icon: ImageVector, val colors: List<Color>, val group: String) {
    Overview("概览", Ico.overview, grad(0xFF8E8E93, 0xFF636366), ""),
    // On the computer, from this phone (Hub.kt): their settings here.
    Calls("通话", Ico.call, grad(0xFF5EE07A, 0xFF30B94D), "手机"),
    Messages("短信", Ico.message, grad(0xFF5EE07A, 0xFF30B94D), "手机"),
    Photos("照片", Ico.photo, grad(0xFFFFD60A, 0xFFFF9F0A), "手机"),
    FileAccess("文件访问", Ico.folder, grad(0xFF64D2FF, 0xFF0A84FF), "手机"),
    Screen("屏幕", Ico.display, grad(0xFF5AC8FA, 0xFF0A84FF), "显示器"),
    Cursor("光标", Ico.cursor, grad(0xFFFFB340, 0xFFFF9500), "输入"),
    Keyboard("键盘", Ico.keyboard, grad(0xFFD1D1D6, 0xFF8E8E93), "输入"),
    Remote("远程", Ico.remote, grad(0xFFBF5AF2, 0xFF8944AB), "输入"),
    Clipboard("剪贴板", Ico.clipboard, grad(0xFF64D2FF, 0xFF30B0C7), "分享"),
    Notifications("通知", Ico.bell, grad(0xFFFF6961, 0xFFFF3B30), "分享"),
    Files("文件", Ico.files, grad(0xFF64D2FF, 0xFF0A84FF), "分享"),
    Sound("声音", Ico.audio, grad(0xFFFF6482, 0xFFFF2D55), "更多"),
    Camera("摄像头", Ico.camera, grad(0xFFBF5AF2, 0xFF8944AB), "更多"),
    General("常规", Ico.general, grad(0xFFAEAEB2, 0xFF636366), "bottom"),
    About("关于", Ico.about, grad(0xFF64D2FF, 0xFF0A84FF), "bottom"),
}

private val BOTTOM = listOf(Tab.Overview, Tab.Screen, Tab.Cursor, Tab.Files)

// The sidebar's green dot: the page's feature is on.
private fun isOn(core: Core, t: Tab): Boolean = when (t) {
    Tab.Screen -> core.screen.viewing
    Tab.Cursor -> core.kvm.linked && core.sync.get("cursor", "in")
    Tab.Keyboard -> core.sync.value("keyboard") == "true"
    Tab.Remote -> core.remote.active
    Tab.Clipboard -> core.sync.get("clipboard", "in") || core.sync.get("clipboard", "out")
    Tab.Notifications -> core.sync.get("notifs", "in") || core.sync.get("notifs", "out")
    Tab.Sound -> core.sync.get("audio", "in") || core.sync.get("audio", "out")
    Tab.Camera -> core.adb.camera.on
    Tab.Calls -> core.hub.callsOn
    Tab.Messages -> core.hub.messagesOn
    Tab.Photos -> core.hub.mediaOn
    Tab.FileAccess -> core.hub.filesOn
    else -> false
} && core.host != null

private fun them(core: Core) = core.host?.name ?: core.hosts().firstOrNull()?.name ?: "电脑"

// Pages that can't work over Bluetooth and don't move the session onto Wi-Fi by themselves
// (sound streams over the network; the computer's Photos): shown as they are while the session
// is on Bluetooth, saying so, with Connect with Wi-Fi Direct (WifiGate).
private fun wifiOnly(core: Core, t: Tab) = (t == Tab.Sound || t == Tab.Photos) && core.host != null && core.link.ble

@Composable
fun App(core: Core, resumed: Int) {
    val v by core.version.collectAsState()
    @Suppress("UNUSED_EXPRESSION") v; @Suppress("UNUSED_EXPRESSION") resumed // re-read Core on every bump
    var tab by rememberSaveable { mutableStateOf(Tab.Overview) }
    var more by rememberSaveable { mutableStateOf(false) }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    // Wireless debugging went off (MIUI / HyperOS): ask once per time it happens.
    var adbAsked by rememberSaveable { mutableStateOf(false) }
    if (!core.adb.off) adbAsked = false
    if (core.adb.off && !adbAsked && !core.link.ble) { // over Bluetooth it can't be used anyway
        val name = them(core)
        AlertDialog(
            onDismissRequest = { adbAsked = true },
            containerColor = pal.card,
            title = { Text("开启无线调试", color = pal.text) },
            text = {
                Text(
                    "$name uses it for this device’s screen, its mouse and keyboard here, and sound. " +
                        "Some phones (MIUI, HyperOS) switch it off after a Wi-Fi change, a screen lock or a restart.\n\n" +
                        "设置将在「开发者选项」中打开：开启 USB 调试和无线调试，然后返回。",
                    color = pal.muted,
                )
            },
            confirmButton = { TextButton(onClick = { adbAsked = true; core.adb.openSettings() }) { Text("打开设置", color = pal.accent) } },
            dismissButton = { TextButton(onClick = { adbAsked = true }) { Text("暂不", color = pal.muted) } },
        )
    }

    // A computer wants to pair, or its code is up: over whatever page is open, not only the
    // notification and the Overview.
    core.invite?.let { inv ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = pal.card,
            title = { Text("${inv.name} 请求配对", color = pal.text) },
            text = { Text("Press Pair to pair this device with ${inv.name}. You’ll check that both show the same code.", color = pal.muted) },
            confirmButton = { TextButton(onClick = { core.pair(inv) }) { Text("配对", color = pal.accent) } },
            dismissButton = { TextButton(onClick = { core.dismissInvite() }) { Text("暂不", color = pal.muted) } },
        )
    }
    core.link.pair?.takeIf { it.phase == "code" && !it.wired }?.let { p ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = pal.card,
            title = { Text("Pair with ${p.name}?", color = pal.text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Check that ${p.name} shows the same code.", color = pal.muted)
                    Text("${p.code.take(3)} ${p.code.drop(3)}", fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = pal.text)
                }
            },
            confirmButton = { TextButton(onClick = { core.link.confirmCode() }) { Text("配对码一致", color = pal.accent) } },
            dismissButton = { TextButton(onClick = { core.link.cancelPairing() }) { Text("取消", color = pal.muted) } },
        )
    }

    // Setup first while something needed isn't allowed (Skip for now remembers what was skipped).
    val ctx = LocalContext.current
    var setupDone by rememberSaveable { mutableStateOf(false) }
    if (!setupDone && SetupCheck.shouldShow(ctx, core)) return SetupScreen(core, ctx, v + resumed) { setupDone = true }
    // The wireless debugging guide when opened from General › Setup / Cursor (Overview has it as a card).
    if (Guide.open.value) return WirelessGuide(core, ctx, v + resumed)

    Box(Modifier.fillMaxSize().background(pal.content)) {
        if (wide) {
            Row(Modifier.safeDrawingPadding()) {
                Sidebar(core, v, tab) { tab = it }
                Page(core, v, tab, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.safeDrawingPadding()) {
                TopIdentity(core, v)
                if (more) MoreList(core, v, Modifier.weight(1f)) { tab = it; more = false }
                else Page(core, v, tab, Modifier.weight(1f))
                NavigationBar(containerColor = pal.card) {
                    val item = @Composable { label: String, icon: ImageVector, colors: List<Color>, selected: Boolean, go: () -> Unit ->
                        NavigationBarItem(
                            selected = selected, onClick = go,
                            icon = { Box(Modifier.alpha(if (selected) 1f else 0.72f)) { Tile(icon, colors, 28) } },
                            label = { Text(label, maxLines = 1, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(selectedTextColor = pal.accent, indicatorColor = pal.accentSoft, unselectedTextColor = pal.muted),
                        )
                    }
                    for (t in BOTTOM) item(t.title, t.icon, t.colors, !more && tab == t) { tab = t; more = false }
                    item("更多", Ico.more, grad(0xFFAEAEB2, 0xFF636366), more || tab !in BOTTOM) { more = true }
                }
            }
        }
    }
}

// The top of the sidebar: the logo, TwinDesk, and who is connected (the Mac's `.identity`).
@Composable
private fun Identity(core: Core, v: Int, logo: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Logo(logo)
        Column(Modifier.padding(start = 12.dp)) {
            Text("TwinDesk", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = pal.text)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val h = core.host
                Dot(when { h != null -> pal.good; core.link.pair != null || core.link.state == "connecting" -> pal.warn; else -> pal.faint }, 7)
                Text(
                    when {
                        h != null -> h.name + " · " + core.net.via + (core.link.rttMs?.let { " · $it ms" } ?: "")
                        core.link.pair != null -> "正在配对…"
                        core.link.state == "connecting" -> "正在连接…"
                        else -> "未连接"
                    },
                    color = pal.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Sidebar(core: Core, v: Int, tab: Tab, go: (Tab) -> Unit) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Column(Modifier.width(250.dp).fillMaxHeight().background(pal.sidebar).verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp)) {
        Box(Modifier.padding(start = 4.dp, bottom = 18.dp)) { Identity(core, v, 54) }
        var group: String? = null
        for (t in Tab.entries) {
            if (t.group == "bottom" && group != "bottom") Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth().height(1.dp).background(pal.line))
            else if (group != null && t.group != group) Spacer(Modifier.height(10.dp))
            group = t.group
            NavItem(t.icon, t.colors, t.title, tab == t, isOn(core, t)) { go(t) }
        }
    }
}

@Composable
private fun TopIdentity(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Box(Modifier.fillMaxWidth().background(pal.sidebar).padding(horizontal = 16.dp, vertical = 10.dp)) { Identity(core, v, 40) }
}

@Composable
private fun MoreList(core: Core, v: Int, modifier: Modifier, go: (Tab) -> Unit) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
        PageHead("更多", "")
        MacCard {
            Tab.entries.filter { it !in BOTTOM }.forEachIndexed { i, t ->
                Column {
                    if (i > 0) Box(Modifier.padding(start = 58.dp).fillMaxWidth().height(1.dp).background(pal.line))
                    Row(Modifier.fillMaxWidth().clickable { go(t) }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Tile(t.icon, t.colors, 30)
                        Text(t.title, color = pal.text, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        if (wifiOnly(core, t)) Text("Wi-Fi Direct", color = pal.muted, fontSize = 12.sp)
                        if (isOn(core, t)) Dot(pal.good, 7)
                        Text("›", color = pal.faint, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Page(core: Core, v: Int, tab: Tab, modifier: Modifier) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Column(modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Column(Modifier.widthIn(max = 820.dp)) {
            when (tab) {
                Tab.Overview -> OverviewPage(core, v)
                Tab.Calls -> CallsPage(core, v)
                Tab.Messages -> MessagesPage(core, v)
                Tab.Photos -> PhotosPage(core, v)
                Tab.FileAccess -> FileAccessPage(core, v)
                Tab.Screen -> ScreenPage(core, v)
                Tab.Cursor -> CursorPage(core, v)
                Tab.Keyboard -> KeyboardPage(core, v)
                Tab.Remote -> RemotePage(core, v)
                Tab.Clipboard -> ClipboardPage(core, v)
                Tab.Notifications -> NotificationsPage(core, v)
                Tab.Files -> FilesPage(core, v)
                Tab.Sound -> SoundPage(core, v)
                Tab.Camera -> CameraPage(core, v)
                Tab.General -> GeneralPage(core, v)
                Tab.About -> AboutPage(core, v)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Primary(label: String, onClick: () -> Unit) = Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) { Text(label) }

@Composable
private fun Secondary(label: String, onClick: () -> Unit) = OutlinedButton(onClick = onClick, shape = RoundedCornerShape(8.dp)) { Text(label, color = pal.text) }

@Composable
private fun NotConnected(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Note("未连接。请在「概览」页连接到 ${them(core)}。", "warn")
}

// ---- Overview ----

@Composable
private fun OverviewPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val link = core.link
    val host = core.host
    val me = core.store.deviceName
    val ip = remember(v) { core.adb.address() } // this phone's Wi-Fi address, for pairing by address on the computer
    if (LocalConfiguration.current.screenWidthDp >= 600) PageHead("TwinDesk", listOf(me, ip).filter { it.isNotEmpty() }.joinToString(" · "))
    else PageHead(me, listOf("这台设备", ip).filter { it.isNotEmpty() }.joinToString(" · "))

    core.invite?.let { inv ->
        Note("${inv.name} wants to pair with this device.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            Primary("配对") { core.pair(inv) }
            Secondary("暂不") { core.dismissInvite() }
        }
    }
    link.pair?.let { p ->
        MacCard("Pairing with ${p.name}") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (p.phase) {
                    "code", "waiting" -> if (p.wired) {
                        Text("On the USB cable. Click Allow on ${p.name}.", color = pal.text)
                        Secondary("取消") { link.cancelPairing() }
                    } else {
                        Text("Check that ${p.name} shows the same code.", color = pal.text)
                        Text("${p.code.take(3)} ${p.code.drop(3)}", fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = pal.text)
                        if (p.phase == "code") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Primary("配对码一致") { link.confirmCode() }
                            Secondary("取消") { link.cancelPairing() }
                        } else Text("Waiting for Allow on ${p.name}…", color = pal.muted)
                    }
                    "failed" -> {
                        Text(p.error, color = pal.bad)
                        Secondary("确定") { link.cancelPairing() }
                    }
                    else -> {
                        Text(if (p.ble) "正在通过蓝牙连接…" else "正在连接…", color = pal.muted)
                        Secondary("取消") { link.cancelPairing() }
                    }
                }
            }
        }
    }

    // The hero: this device, the link, the computer (the Mac's `.hero`).
    MacCard {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Device(me, phone = true, Modifier.weight(1f))
            Column(Modifier.weight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(
                    if (host != null) Brush.horizontalGradient(listOf(pal.good, pal.accent)) else Brush.horizontalGradient(listOf(pal.trackOff, pal.trackOff)),
                ))
                Text(
                    when { host != null -> listOfNotNull(core.net.via, link.rttMs?.let { "$it ms" }).joinToString(" · ").ifEmpty { "已连接" }; link.state == "connecting" -> "正在连接…"; link.pair != null -> "正在配对…"; else -> "未连接" },
                    color = pal.muted, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
                )
            }
            Device(them(core), phone = false, Modifier.weight(1f))
        }
        Text(
            when {
                host != null -> "已连接"
                link.message.isNotEmpty() -> link.message
                core.hosts().isNotEmpty() -> "点「连接」，或在 Mac 上点击这台设备。"
                else -> "在下方选择一台电脑，或按地址配对。"
            },
            color = pal.muted, fontSize = 12.5.sp, lineHeight = 17.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        )
        // Wireless debugging not set up (or off): one line under Connected, with the way to fix it.
        // Reached anyway (adb's TCP mode, on a direct Wi-Fi link): nothing to fix.
        // Over Bluetooth: greyed out, it only works over Wi-Fi.
        val adbBle = core.link.ble
        if (host != null && !core.adb.paired && (adbBle || core.adb.setupNeeded || !core.adb.wifiDebugging)) Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp).clip(RoundedCornerShape(8.dp)).background(if (adbBle) pal.line.copy(alpha = 0.35f) else pal.warn.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (adbBle) "Wireless debugging: only available over Wi-Fi. You’re connected to ${host.name} over Bluetooth."
                else if (core.adb.setupNeeded) "Wireless debugging isn’t set up. ${host.name} needs it to show this screen and control it through ADB."
                else "Wireless debugging is off. ${host.name} needs it to show this screen and control it through ADB.",
                color = if (adbBle) pal.muted else pal.text, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            if (!adbBle) { if (core.adb.setupNeeded) Secondary("设置") { Guide.open.value = true } else Secondary("开启") { core.adb.turnOn() } }
        }
        if (host != null) {
            val show = { core.ctx.startActivity(Intent(core.ctx, ViewerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            // On Bluetooth: a direct Wi-Fi link on request, or made for files and screens: Back to Bluetooth ends it (both sides).
            val onWifi = core.net.wanted("user") || core.net.linked
            val wifiLabel = if (onWifi) "返回蓝牙" else if (core.link.ble) "通过 Wi-Fi 直连连接" else null
            val wifiClick = { if (onWifi) core.net.drop() else core.net.need("user"); core.changed() }
            if (LocalConfiguration.current.screenWidthDp >= 600) {
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    Primary("显示 ${host.name} 的屏幕", show)
                    if (wifiLabel != null) Secondary(wifiLabel) { wifiClick() }
                    Secondary("断开连接") { core.disconnect() }
                }
            } else {
                // A phone: one button per line, inside the card's padding.
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = show, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White)) {
                        Text("显示 ${host.name} 的屏幕", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (wifiLabel != null) OutlinedButton(onClick = { wifiClick() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text(wifiLabel, color = pal.text) }
                    OutlinedButton(onClick = { core.disconnect() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("断开连接", color = pal.text) }
                }
            }
        }
    }


    // The other paired computers, while connected: Switch (Take over when nearby) lets this one go and connects that one.
    if (host != null) {
        val others = core.hosts().filter { it.id != host.id }
        if (others.isNotEmpty()) MacCard("切换到") {
            others.forEachIndexed { i, h ->
                val near = core.isNear(h.id)
                MacRow(h.name, if (near && !core.discovery.found.containsKey(h.id)) "蓝牙在附近 · 已配对" else if (near) "在附近 · 已配对" else "已配对 · 不在附近", i == 0) {
                    Secondary(if (near) "接管" else "切换") { core.switchTo(h) }
                }
            }
        }
        MacCard { BleSwitch(core, "请在「通用」页允许「附近的设备」", first = true) }
    }

    if (host != null) {
        val wide = LocalConfiguration.current.screenWidthDp >= 840
        val m2p = @Composable { mod: Modifier ->
            MacCard(modifier = mod) {
                DirHead(host.name, false, me, true)
                SwitchRow("屏幕", "在「屏幕」页打开它", core.screen.viewing, first = true) { if (it) core.ctx.startActivity(Intent(core.ctx, ViewerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                SwitchRow("声音", "", core.sync.get("audio", "in")) { core.sync.set("audio", "in", it) }
                SwitchRow("鼠标和键盘", "", core.sync.get("cursor", "in")) { core.sync.set("cursor", "in", it) }
                SwitchRow("剪贴板", "", core.sync.get("clipboard", "in")) { core.sync.set("clipboard", "in", it) }
                SwitchRow("通知", "", core.sync.get("notifs", "in")) { core.sync.set("notifs", "in", it) }
            }
        }
        val p2m = @Composable { mod: Modifier ->
            MacCard(modifier = mod) {
                DirHead(me, true, host.name, false)
                SwitchRow("声音", soundHint(core), core.sync.get("audio", "out"), first = true) { core.sync.set("audio", "out", it) }
                SoundStart(core)
                SwitchRow("剪贴板", "", core.sync.get("clipboard", "out")) { core.sync.set("clipboard", "out", it) }
                SwitchRow("通知", if (core.notifs.hasAccess) "" else if (!core.notifs.supported) "需要带通知功能的 TwinDesk（见「通知」页）" else "需要「通知使用权」（通用）", core.sync.get("notifs", "out")) { core.sync.set("notifs", "out", it) }
                // Calls, Messages and Photos, when the computer has them: what each needs is asked for as it's switched on.
                if (core.hub.peer) for (what in listOf("calls", "messages", "media", "files")) {
                    val perm = core.hub.perm(what)
                    val on = core.hub.on(what)
                    SwitchRow(
                        Hub.label(what),
                        when {
                            perm == "none" && what == "files" -> "需要 Android 11 或更高版本"
                            perm == "none" -> "需要带通知功能的 TwinDesk（见「通知」页）"
                            on && perm == "ask" && what == "files" -> "请允许「所有文件访问」：重新打开开关"
                            on && perm == "ask" -> "在弹窗中点「允许」，或重新打开开关"
                            on && perm == "partial" && what == "media" -> "仅你选择的照片"
                            on && perm == "partial" -> "缺少部分权限：重新打开开关以允许"
                            what == "calls" -> "在 ${host.name} 上接听和拨打电话"
                            what == "messages" -> "在 ${host.name} 上读取和发送短信"
                            what == "files" -> "在 ${host.name} 上浏览这台手机的存储，无需 ADB"
                            else -> "在 ${host.name} 上查看并保存"
                        },
                        on, enabled = perm != "none",
                    ) {
                        core.hub.set(what, it)
                        if (it && perm != "ok") core.ctx.startActivity(Intent(core.ctx, PermActivity::class.java).putExtra("what", what).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
        }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { m2p(Modifier.weight(1f)); p2m(Modifier.weight(1f)) }
        else { m2p(Modifier); p2m(Modifier) }
    }

    if (host == null) MacCard("附近") {
        val hosts = core.hosts()
        var first = true
        for (h in hosts) {
            val near = core.isNear(h.id)
            MacRow(h.name, if (link.state == "connecting" && link.host?.id == h.id) "正在连接…" else if (near && !core.discovery.found.containsKey(h.id)) "蓝牙在附近 · 已配对" else if (near) "在附近 · 已配对" else "已配对 · 不在附近", first) {
                TextButton(onClick = { core.forget(h.id) }) { Text("取消配对", color = pal.bad) }
                // Nearby: Take over (a phone's hello takes the computer from whoever has it).
                Primary(if (near) "接管" else "连接") { core.connect(h) }
            }
            first = false
        }
        for (f in core.nearby().filter { f -> hosts.none { it.id == f.id } }) {
            MacRow(f.name, "${if (f.os == "mac") "Mac" else if (f.os == "linux") "Linux" else "PC"} · ${if (f.address.startsWith("ble:") || f.address.startsWith("blp:")) "Bluetooth" else f.address}", first) { Secondary("配对") { core.pair(f) } }
            first = false
        }
        if (first) MacRow("正在寻找电脑…", if (core.bleOn && core.ble.allowed()) "在电脑上打开 TwinDesk（同一 Wi-Fi，或蓝牙已开启）" else "在电脑上打开 TwinDesk（同一 Wi-Fi）", true)
        BleSwitch(core, "请在「通用」页允许「附近的设备」")
        var addr by remember { mutableStateOf("") }
        Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(pal.line))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("按地址配对", color = pal.text, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(addr, { addr = it }, placeholder = { Text("192.168.1.20", color = pal.faint) }, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = pal.text), shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                Secondary("配对") { if (addr.isNotBlank()) core.pairAddress(addr) }
            }
        }
    }
}

// Bluetooth with computers (Overview and General, as on the computers): for one that isn't on this
// network. Off by itself while the session is on the same Wi-Fi, on again when that drops (Core.bleTick).
@Composable
private fun BleSwitch(core: Core, askHint: String, first: Boolean = false) {
    val ctx = LocalContext.current
    val name = them(core)
    val link = core.link
    val allowed = (Ble.permissions() + BleServe.permissions() + Direct.permission()).all { ctx.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    SwitchRow("与电脑的蓝牙连接", when {
        !core.bleOn -> "仅此网络上的电脑或 USB 线可以连接"
        !core.ble.radioOn -> "这台设备的蓝牙已关闭"
        !allowed -> askHint
        core.bleWifiOff && link.connected && core.net.via == "Wi-Fi" -> "与 $name 在同一 Wi-Fi 时关闭，断开后自动恢复"
        else -> "当 $name 不在同一网络时通过蓝牙连接。文件和屏幕会临时切换到 Wi-Fi 直连"
    }, core.bleOn, first = first) { core.store.setFlag("ble", it); core.changed() }
}

@Composable
private fun Device(name: String, phone: Boolean, modifier: Modifier) {
    val k = if (LocalConfiguration.current.screenWidthDp >= 600) 1f else 0.72f // smaller drawings on a phone
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (phone) Box(Modifier.width((34 * k).dp).height((60 * k).dp).clip(RoundedCornerShape(8.dp)).border(3.dp, pal.text.copy(alpha = 0.85f), RoundedCornerShape(8.dp)))
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width((84 * k).dp).height((54 * k).dp).clip(RoundedCornerShape(6.dp)).border(3.dp, pal.text.copy(alpha = 0.85f), RoundedCornerShape(6.dp)))
            Box(Modifier.padding(top = 3.dp).width((104 * k).dp).height(5.dp).clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)).background(pal.text.copy(alpha = 0.85f)))
        }
        Text(name, fontWeight = FontWeight.SemiBold, color = pal.text, fontSize = 13.sp, maxLines = 2, lineHeight = 16.sp, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
            softWrap = name.contains(' '), modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp))
    }
}

// ---- Wi-Fi Direct ----

// Over Bluetooth, a page that needs Wi-Fi says so and offers the way there (the Overview's
// Connect with Wi-Fi Direct): the page itself stays as it is.
@Composable
private fun WifiGate(core: Core, what: String) {
    if (core.host == null || !core.link.ble) return
    val n = core.net
    val moving = n.state in listOf("asking", "hosting", "joining")
    Note(
        when {
            moving -> "$what 仅支持 Wi-Fi Direct。正在连接…"
            n.state == "failed" -> "$what is only available over Wi-Fi Direct. ${n.error.ifEmpty { "Wi-Fi Direct didn’t come up" }}"
            else -> "$what 仅支持 Wi-Fi Direct（或同一 Wi-Fi 网络）：你当前通过蓝牙连接。"
        },
        "warn",
    )
    if (!moving) Row(Modifier.padding(bottom = 14.dp)) {
        Primary(if (n.state == "failed") "重试" else "通过 Wi-Fi 直连连接") { core.net.need("user"); core.changed() }
    }
}

// ---- Calls, Messages, Photos, File access (on the computer, from this phone: Hub.kt) ----

// The top of each: the switch (the computer keeps it), and what's missing here, with Allow.
@Composable
private fun HubHead(core: Core, v: Int, what: String, title: String, desc: String): Boolean {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val hub = core.hub
    val name = them(core)
    val perm = hub.perm(what)
    val on = hub.on(what)
    PageHead(title, desc, if (core.host != null && hub.peer && perm != "none") on else null) {
        hub.set(what, it)
        if (it && perm != "ok") hub.askHere(what)
    }
    if (core.host == null) { NotConnected(core, v); return false }
    if (!hub.peer) { Note("$name 上的 TwinDesk 需为最新版本才能使用$title。", "warn"); return false }
    when {
        perm == "none" && what == "files" -> Note("文件访问需要 Android 11 或更高版本。可以改用 ADB 在 $name 上挂载。", "warn")
        perm == "none" -> Note("$title 需要带通知功能的 TwinDesk：Android 不允许下载安装的应用读取短信。无线调试配置好后 $name 会安装它（其「手机」页 › 安装）。你的设置和配对都会保留。", "warn")
        !on -> Note("已关闭。请在这里或 $name 上开启。")
        perm == "ask" -> {
            Note(
                when (what) {
                    "files" -> "允许「所有文件访问」，让 $name 能浏览这台手机的存储。TwinDesk 自己读写文件：无需 ADB。"
                    "media" -> "允许访问照片和视频，这样 $name 才能显示它们。"
                    else -> "Allow what $title needs: ${Hub.wanted(what).size} permissions, asked for once."
                },
                "warn",
            )
            Row(Modifier.padding(bottom = 14.dp)) { Primary("允许") { hub.askHere(what) } }
        }
        perm == "partial" -> {
            Note(if (what == "media") "只有你选择的照片会显示在 $name 上。" else "$title 还需要更多权限：请允许其余权限以完整使用。")
            Row(Modifier.padding(bottom = 14.dp)) { Secondary("允许更多") { hub.askHere(what) } }
        }
        else -> Note("开启：在这台手机上使用$title，显示在 $name。")
    }
    return on && perm != "none"
}

@Composable
private fun CallsPage(core: Core, v: Int) {
    val name = them(core)
    if (!HubHead(core, v, "calls", "通话", "在 ${them(core)} 上接听、拨打和挂断这台手机的通话。")) return
    MacCard("在 $name 上") {
        MacRow("接听和拒接", "A window on $name when this phone rings", first = true)
        MacRow("拨打电话", "$name 的通话页上有数字键盘、联系人和最近通话")
    }
    // The computer as this phone's headset: paired over classic Bluetooth by the person, in Bluetooth
    // settings (TwinDesk doesn't pair them itself). Forget at the bottom.
    val hub = core.hub
    if (hub.btAudio) MacCard("在 $name 上播放声音") {
        val bt = hub.btState()
        val allowed = bt.optBoolean("allowed", true)
        val bonded = bt.optBoolean("bonded")
        MacRow(
            "Bluetooth",
            when {
                !allowed -> "允许「附近的设备」，让 TwinDesk 能看到这台手机与 $name 的配对"
                !bt.optBoolean("radio", true) -> "手机的蓝牙已关闭"
                bt.optBoolean("bonding") -> "配对中… 请在此处和 $name 上确认配对码"
                bt.optBoolean("forget") -> "请在本机蓝牙设置中取消与 $name 的配对（Android 会自己保留旧配对），然后在 $name 上重新配对"
                bonded -> "已与 $name 配对：可以在那边接听通话"
                bt.optBoolean("le") -> "仅通过蓝牙 LE 配对，无法承载通话：请「取消配对」，然后在蓝牙设置中重新配对"
                !hub.btKnown -> "正在等待 $name…"
                else -> "尚未与 $name 建立经典蓝牙配对：请在蓝牙设置中配对（本机：配对新设备；$name：添加设备）。配对完成后 TwinDesk 会自动连接"
            },
            first = true,
        ) {
            if (!allowed) Primary("允许") { hub.askHere("bt") }
            else if (hub.btKnown && !bonded && !bt.optBoolean("bonding")) Secondary("打开蓝牙设置") { hub.btSettings() }
        }
        var sure by remember { mutableStateOf(false) }
        MacRow("取消配对", "This phone and $name forget each other, over classic Bluetooth and LE; Bluetooth settings open to pair them again") {
            TextButton(onClick = { if (sure) { sure = false; hub.forgetBt() } else { sure = true } }) { Text(if (sure) "取消配对？" else "取消配对", color = pal.bad) }
        }
    }
}

@Composable
private fun MessagesPage(core: Core, v: Int) {
    val name = them(core)
    if (!HubHead(core, v, "messages", "短信", "在 ${them(core)} 上读取和发送这台手机的短信。")) return
    MacCard("在 $name 上") {
        MacRow("会话", "$name 的短信页可查看短信和彩信图片", first = true)
        MacRow("新短信", "在 $name 的「短信」标签中统计")
    }
}

@Composable
private fun PhotosPage(core: Core, v: Int) {
    val name = them(core)
    WifiGate(core, "在 $name 上查看照片")
    if (!HubHead(core, v, "media", "照片", "在 ${them(core)} 上查看这台手机的照片和视频，并复制过去。")) return
    MacCard("在 $name 上") {
        MacRow("照片、视频和截图", "$name 的照片页按最新优先排序", first = true)
        MacRow("保存", "复制的内容会保存到 $name 的「下载」中")
    }
}

@Composable
private fun FileAccessPage(core: Core, v: Int) {
    val name = them(core)
    WifiGate(core, "$name 上的手机文件")
    if (HubHead(core, v, "files", "文件访问", "在 ${them(core)} 上浏览这台手机的存储，并双向复制文件。")) MacCard("在 $name 上") {
        MacRow("手机文件", "文件夹、保存到 $name、上传、新建文件夹、重命名、删除", first = true)
        MacRow("无需 ADB", "TwinDesk 通过「所有文件访问」权限自行读写文件。在 $name 上挂载是走 ADB 的方式")
        MacRow("位置", "通过 USB、Wi-Fi 或 Wi-Fi Direct 连接。走蓝牙时 $name 会提示，并给出「用 Wi-Fi Direct 连接」的选项")
    }
    if (core.host != null) SharedFolders(core, v)
}

// Shared folders, both ways (FolderShare.kt): this phone's in the computer's file manager, the
// computer's in the Files app here.
@Composable
private fun SharedFolders(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val ctx = LocalContext.current
    val sh = core.share
    val name = them(core)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) sh.choose(uri) }
    val ble = core.link.ble
    MacCard("共享文件夹") {
        SwitchRow(
            "与 $name 共享文件夹",
            when {
                !sh.allowed -> "需要「所有文件访问」权限（见上方）"
                ble && sh.on -> "${sh.shown()}: $name opens it over Wi-Fi Direct"
                sh.error.isNotEmpty() -> "Couldn’t share: ${sh.error}"
                sh.on && sh.serving -> "${sh.shown()}: open on $name (Phone Files › Shared folders)"
                else -> sh.shown()
            },
            sh.on, first = true,
        ) {
            sh.setOn(it)
            if (it && !sh.allowed) core.hub.askHere("files")
        }
        MacRow("文件夹", sh.shown()) { Secondary("选择…") { pick.launch(null) } }
        val pc = sh.pc
        MacRow(
            "$name 的共享文件夹",
            when {
                ble && sh.opening -> "正在通过 Wi-Fi 直连连接…"
                ble && sh.pcServe -> "「打开」会通过 Wi-Fi 直连连接"
                pc != null -> "“${pc.name}” in the Files app and every app’s file picker"
                sh.pcServe -> "Opening…"
                else -> "关闭：开启后可在「文件」应用中看到 $name 的文件夹"
            },
        ) {
            if (pc != null) Primary("打开") { openPcShare(ctx) }
            else if (ble && sh.pcServe && !sh.opening) Primary("打开") { sh.openOverWifi() }
            MacSwitch(sh.pcServe, { sh.setPcServe(it) })
        }
    }
}

// The Files app at the computer's shared folder (its root; else the system's file picker there).
fun openPcShare(ctx: android.content.Context) {
    val root = android.provider.DocumentsContract.buildRootUri(PcShareProvider.authority(ctx), PcShareProvider.ROOT)
    val doc = android.provider.DocumentsContract.buildDocumentUri(PcShareProvider.authority(ctx), "/")
    val tries = listOf(
        Intent(Intent.ACTION_VIEW).setDataAndType(root, "vnd.android.document/root"),
        Intent(Intent.ACTION_VIEW).setDataAndType(doc, android.provider.DocumentsContract.Document.MIME_TYPE_DIR),
        Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*").putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, doc),
    )
    for (i in tries) if (runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.isSuccess) return
}

// ---- Screen ----

// This device's camera on the computer (its Camera tab: phoneview.js openCamera, over wireless
// debugging): its picture in TwinDesk there, and with Virtual camera on there, a camera every app
// there can pick ("TwinDesk 摄像头").
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val host = core.host
    val adb = core.adb
    val cam = adb.camera
    val name = them(core)
    val canShow = adb.paired || adb.known
    var facing by rememberSaveable { mutableStateOf("back") }
    val shown = if (cam.on) cam.facing ?: facing else facing
    PageHead(
        "摄像头",
        "本机摄像头投到 $name：在那边 TwinDesk 中，也可作为其应用可选的摄像头。",
        master = if (host != null && (canShow || cam.on)) cam.on else null,
        onMaster = { adb.cameraOnComputer(it, facing) },
    )
    if (host == null) return NotConnected(core, v)
    MacCard("把此摄像头显示在 $name 上") {
        MacRow(
            "摄像头",
            if (cam.on) "切换需要片刻" else "启动时使用的那个",
            first = true,
        ) {
            SingleChoiceSegmentedButtonRow {
                val sides = listOf("back" to "返回", "front" to "前置")
                sides.forEachIndexed { i, (f, label) ->
                    SegmentedButton(
                        selected = shown == f,
                        onClick = { facing = f; if (cam.on) adb.cameraOnComputer(true, f) },
                        shape = SegmentedButtonDefaults.itemShape(i, sides.size),
                    ) { Text(label) }
                }
            }
        }
        MacRow(
            "在 $name 上",
            when {
                !canShow -> "需要无线调试：请从「概览」页设置"
                cam.error.isNotEmpty() -> "It stopped: ${cam.error}"
                cam.on && cam.virtual -> "正以「TwinDesk Camera」运行：在那边的 Zoom、Teams 或浏览器中只选它"
                cam.on -> "在那边 TwinDesk 的「摄像头」页。其「虚拟摄像头」会让它成为其他应用可选的摄像头"
                else -> "关闭"
            },
        )
    }
    Note(
        if (cam.virtual) "$name 的虚拟摄像头已开启：那边的应用会把这台摄像头识别为「TwinDesk 摄像头」。"
        else "在 $name 上 TwinDesk 的「摄像头」页开启虚拟摄像头，即可把此摄像头作为那边应用（Zoom、Teams、浏览器…）可用的真实摄像头。",
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val host = core.host
    val s = core.screen
    val ctx = LocalContext.current
    val name = them(core)
    PageHead("屏幕", "把本机用作 $name 的屏幕：镜像它的屏幕，或作为第二块屏幕（扩展）。")
    if (host == null) return NotConnected(core, v)
    // The other way round: this device's screen in a window on the computer, over wireless debugging.
    // Paired now or before: the computer brings it back by itself (on Bluetooth, onto Wi-Fi first).
    val adb = core.adb
    val canShow = adb.paired || adb.known
    MacCard("这块屏幕显示到 $name") {
        MacRow(
            "在 $name 上显示这块屏幕",
            when {
                !canShow -> "需要无线调试：请从「概览」页设置"
                adb.viewed -> "本机的窗口已在 $name 上打开。关闭即结束"
                else -> "在那边的窗口中显示，带它的鼠标和键盘"
            },
            first = true,
        ) {
            if (adb.viewed) Secondary("关闭") { adb.showOnComputer(false) }
            Button(onClick = { adb.showOnComputer(true) }, enabled = canShow, colors = ButtonDefaults.buttonColors(containerColor = pal.accent, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) { Text("显示") }
        }
    }
    Note(
        when {
            !s.sharing -> "$name 没有共享屏幕。"
            s.mode == "extend" -> "$name 共享了一块与本机同样大小的第二屏幕（扩展）。"
            else -> "$name 正在共享它自己的屏幕（镜像）。"
        },
    )
    MacCard {
        MacRow("显示 $name 的屏幕", "全屏显示。点按顶部的手柄可切换镜像、扩展和键盘", first = true) {
            Primary("打开") { ctx.startActivity(Intent(ctx, ViewerActivity::class.java)) }
        }
        if (s.remote) MacRow("模式", "扩展会让 $name 成为与本机同样大小的第二块屏幕") {
            // A phone: Mirror and Extend across the row's width, under the label.
            SingleChoiceSegmentedButtonRow(if (LocalConfiguration.current.screenWidthDp < 600) Modifier.fillMaxWidth() else Modifier) {
                val modes = listOf("mirror" to "镜像", "extend" to "扩展")
                modes.forEachIndexed { i, (m, label) ->
                    SegmentedButton(selected = s.sharing && s.mode == m, onClick = { s.control(share = true, mode = m) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(label) }
                }
            }
        } else MacRow("模式", "$name 自行开始和切换共享")
        if (s.sharing && s.remote) MacRow("共享", "") { Secondary("停止") { s.control(share = false) } }
    }
    MacCard("触摸") {
        val pad = core.store.flag("screen.touchpad", true)
        SwitchRow("触控板", "滑动移动指针；滑动绝不触发点击。关闭后：指针跟随触摸点", pad, first = true) { s.setOpt("screen.touchpad", it) }
        if (pad) {
            MacRow("滑动", "移动指针")
            MacRow("点按", if (core.remote.tapClick) "左键" else "无（「遥控 › 点按点击」已关闭）")
            MacRow("长按", "右键")
            MacRow("双指", "滚动")
        } else {
            MacRow("点按", "单击")
            MacRow("长按", "右键")
            MacRow("拖拽", "拖拽（选择、移动窗口）")
            MacRow("双指", "滚动")
        }
        if (!s.inputOn) MacRow("输入已关闭", "$name 未接受本机的输入（其「共享屏幕」›「接收输入」）")
    }
    MacCard("手写笔") {
        SwitchRow("笔", "压力、倾斜、橡皮擦和侧键，像在 $name 上使用绘图板；笔靠近时手部触控不生效。关闭：手写笔仅当作鼠标。", core.store.flag("screen.pen", true), first = true) { s.setOpt("screen.pen", it) }
    }
    MacCard("播放") {
        SwitchRow("把 $name 的声音随屏幕一起传输", "当 $name 随屏幕共享声音时", core.store.flag("screen.sound", false), first = true) { s.setOpt("screen.sound", it) }
        SwitchRow("流畅", "平滑动态，约延迟一帧。关闭后：每帧到达即显示", core.store.flag("screen.smooth", false)) { s.setOpt("screen.smooth", it) }
    }
}

// ---- Cursor ----

@Composable
private fun CursorPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val host = core.host
    val name = them(core)
    val me = core.store.deviceName
    // Over Bluetooth (pair.sync "ble", the computer's kvm.js): off, nothing crosses while the session runs through Bluetooth.
    val paused = core.link.ble && core.sync.value("ble") == "false"
    val shared = core.kvm.linked && core.sync.get("cursor", "in") && !paused
    PageHead("光标", "把 $name 的指针移出其屏幕边缘即可使用这台设备。", if (host != null) shared else null) {
        // Through Bluetooth, with cursor sharing on, the switch is Over Bluetooth: off there leaves it on for Wi-Fi.
        if (core.link.ble && core.kvm.linked && core.sync.get("cursor", "in")) core.sync.setValue("ble", it)
        else {
            if (it && !core.sync.get("cursor", "in")) core.sync.set("cursor", "in", true)
            if (it && core.link.ble) core.sync.setValue("ble", true)
            core.kvm.setLinked(it)
        }
    }
    if (host == null) return NotConnected(core, v)
    // Accessibility (chosen, or a session on Bluetooth): no wireless debugging needed, only the service.
    // ADB reached without wireless debugging (its TCP mode, on a direct Wi-Fi link) counts as there.
    val adbOn = core.adb.wifiDebugging || core.adb.paired
    val viaPhone = core.input.via == "phone" || (core.input.via == "auto" || core.link.ble) && !adbOn
    val adbMissing = !viaPhone && !adbOn
    Note(
        when {
            viaPhone && !core.input.supported -> "This build of TwinDesk can’t use Accessibility: Android’s Play Protect blocks downloaded apps that can. " +
                "请使用无线调试；配置好后，$name 会安装带通知功能的 TwinDesk，它可以使用（在 $name 的「手机」页 ›「安装」）。"
            viaPhone && !core.input.on -> "请在辅助功能中允许 TwinDesk，这样 $name 的指针就能在这里点按和输入。"
            adbMissing -> "$name 通过无线调试把指针移到这里，但无线调试已关闭。也可以在下面选择辅助功能。"
            !core.pointer.allowed -> "请在「常规 › 设置」中允许「显示指针」，以便在这里看到 $name 的指针。"
            !core.kvm.linked -> "Cursor sharing is off. Double-tap ${core.kvm.hotkey} on $name to turn it on."
            paused -> "Cursor sharing is off over Bluetooth. It comes back on Wi-Fi, or double-tap ${core.kvm.hotkey} on $name to turn it on."
            else -> "把 $name 的指针推过边缘即可来到本机。双击 ${core.kvm.hotkey} 取回控制权。"
        },
        if (adbMissing || viaPhone && !core.input.on || !core.pointer.allowed) "warn" else "info",
    )
    if (viaPhone && !core.input.supported) Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!adbOn) Primary("开启无线调试") { core.adb.turnOn() }
        if (core.adb.setupNeeded) Secondary("设置") { Guide.open.value = true }
    }
    else if (viaPhone && !core.input.on) Row(Modifier.padding(bottom = 14.dp)) { Primary("允许") { core.input.openSettings() } }
    else if (adbMissing || !viaPhone && core.adb.setupNeeded) Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (adbMissing) Primary("开启无线调试") { core.adb.turnOn() }
        if (core.adb.setupNeeded) Secondary("设置") { Guide.open.value = true }
    }
    MacCard {
        DirHead(name, false, me, true)
        SwitchRow("在 $me 上使用 $name 的鼠标和键盘", "把指针推过屏幕边缘", core.sync.get("cursor", "in"), first = true) { core.sync.set("cursor", "in", it) }
        MacRow("方式", when {
            core.input.via == "auto" && core.input.effective == "adb" -> "自动：ADB（无线调试已开启）"
            core.input.via == "auto" -> "自动：无线调试开启前使用无障碍服务"
            core.input.via == "phone" -> "无障碍：TwinDesk 自行点按和输入，支持 Wi-Fi、Wi-Fi 直连或蓝牙。无需开发者选项"
            else -> "ADB (wireless debugging): full control. Over Bluetooth without it, Accessibility"
        }) {
            SingleChoiceSegmentedButtonRow {
                val ways = listOf("adb" to "ADB", "phone" to "辅助功能")
                ways.forEachIndexed { i, (w, label) ->
                    SegmentedButton(selected = core.input.effective == w, onClick = { core.input.setVia(w); core.changed() }, shape = SegmentedButtonDefaults.itemShape(i, ways.size)) { Text(label, maxLines = 1) }
                }
            }
        }
    }
    MacCard("$name 在哪个方向") { LayoutPicker(me, name, core.kvm.macSide) { core.kvm.setMacSide(it) } }
    MacCard("手感") {
        val speed = core.sync.value("speed").toFloatOrNull() ?: 1f
        MacRow("此处的指针速度", "", first = true) {
            MacSlider(speed, 0.25f..4f, 14, { "%.2f×".format(it) }) { v ->
                val steps = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f)
                val snapped = steps.minBy { kotlin.math.abs(it - v) }
                core.sync.setValue("speed", if (snapped % 1f == 0f) snapped.toInt() else snapped)
            }
        }
        SwitchRow("反转滚动", "此处的滚动方向反转", core.sync.value("reverseScroll") == "true") { core.sync.setValue("reverseScroll", it) }
        MacRow("右键", "返回")
        MacRow("中键", "主屏")
    }
    MacCard("不在同一网络") {
        SwitchRow("通过蓝牙", "当 $name 不在本网络、你通过蓝牙连接时（较慢）。关闭：在重新连上 Wi-Fi 前不传输任何内容。两边一致",
            core.sync.value("ble") != "false", first = true) { core.sync.setValue("ble", it) }
    }
}

// ---- Keyboard ----

@Composable
private fun KeyboardPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val host = core.host
    val name = them(core)
    val me = core.store.deviceName
    val on = core.sync.value("keyboard") == "true"
    PageHead("键盘", "指针在哪台设备，就在哪台输入。", if (host != null) on else null) { core.sync.setValue("keyboard", it) }
    if (host == null) return NotConnected(core, v)
    Note(if (on) "$name 的键盘在其指针位于此处时可在此输入。" else "键盘共享已关闭：只有指针会来到这里。")
    MacCard("快捷键") {
        MacRow(core.kvm.hotkey, "在 $name 上双击可开启或关闭光标共享。可在 $name 的 TwinDesk 中设置", first = true)
    }
    MacCard {
        DirHead(name, false, me, true)
        MacRow("此处的快捷键", "Cmd+ 相当于 Ctrl，Cmd+C 复制、Cmd+V 粘贴", first = true)
        MacRow("输入", "每个按键都按原样发送，键盘布局和软键盘不会造成干扰")
    }
    MacCard("在屏幕视图中") {
        MacRow("$name’s screen here", "本机的键盘可在 $name 上输入。「键盘」按钮会打开软键盘", first = true)
    }
}

// ---- Remote ----

@Composable
private fun RemotePage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val r = core.remote
    val ctx = LocalContext.current
    val name = them(core)
    val me = core.store.deviceName
    PageHead("远程", "把本机用作 $name 的触控板和键盘，或用作手柄。")
    if (core.host == null) {
        NotConnected(core, v)
        return BtPadCard(core) // works without TwinDesk on the computer
    }
    if (r.unsupported) Note("$name 没有响应遥控：请更新那边的 TwinDesk。", "warn")
    else if (r.allowed == false) Note(r.why.ifEmpty { "$name 不接受本机的输入。请在 $name 的 TwinDesk 中开启「遥控」。" }, "warn")
    val open = { mode: String -> ctx.startActivity(Intent(ctx, RemoteActivity::class.java).putExtra("mode", mode)) }
    MacCard {
        MacRow("触控板和键盘", "滑动可移动 $name 的指针，点按即单击，长按即右键，双指滚动。也支持按键和媒体键", first = true) {
            Primary("打开") { open("pad") }
        }
        MacRow("手柄", "横握时如同手柄：左摇杆是 WASD，右摇杆是鼠标；顶栏的「按键」可设置每个按钮按下的键") {
            Primary("打开") { open("gamepad") }
        }
    }
    MacCard("触控板") {
        MacRow("指针速度", "同时影响手柄右摇杆和「屏幕」页的触控板", first = true) {
            MacSlider(r.speed, 0.3f..3f, 0, { "%.1f×".format(java.util.Locale.ROOT, it) }) { r.setSpeed(it) }
        }
        SwitchRow("点按点击", "快速点按为左键；滑动绝不触发点击。关闭后：仅点击按钮生效", r.tapClick) { r.setTapClick(it) }
        SwitchRow("流畅", "对 $name 的指针移动做平滑处理，约晚一帧。关闭：每次移动立即生效", r.smooth) { r.setSmooth(it) }
    }
    BtPadCard(core)
    Note(
        when {
            core.link.wired -> "经 USB 线连接：玩游戏最快的方式。"
            core.link.ble -> "经蓝牙时指针每秒约移动 30 次。Wi-Fi 或 USB 线会更快。"
            else -> "Wi-Fi 下的速度与应用顶部显示的延迟相当。玩游戏用 USB 线最快。"
        },
    )
}

// As a Bluetooth gamepad (BtPad.kt): any computer, the Mac too, no driver and no TwinDesk there.
@Composable
private fun BtPadCard(core: Core) {
    val ctx = LocalContext.current
    val made = core.btPadMade
    val on = core.store.flag("remote.btpad", false)
    val pad = if (made || on) core.btPad else null
    LaunchedEffect(on) { if (on) core.btPad.start() }
    val findable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { core.changed() }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { got ->
        if (got.values.all { it }) { core.btPad.setOn(true); findable.launch(core.btPad.discoverableIntent()) }
        else ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
    }
    val state = pad?.state ?: "off"
    MacCard("作为蓝牙手柄") {
        SwitchRow("作为蓝牙手柄", "A game controller for any computer over Bluetooth, the Mac too: no driver and no TwinDesk needed there. " +
            "Games that take any controller (Steam, emulators); Xbox-only games on Windows want Real gamepad on the PC instead", on, first = true) { v ->
            if (!v) core.btPad.setOn(false)
            else if (!core.btPad.allowed()) ask.launch(BtPad.permissions())
            else { core.btPad.setOn(true); findable.launch(core.btPad.discoverableIntent()) }
        }
        if (on) {
            MacRow("当前", when (state) {
                "starting" -> "Starting…"
                "unavailable" -> pad?.message ?: ""
                "connected" -> "Connected to ${pad?.hostName}: open the gamepad"
                "ready" -> "已就绪：等待电脑连接"
                else -> pad?.message?.ifEmpty { "关闭" } ?: "关闭"
            }) { if (state == "connected") Primary("打开") { ctx.startActivity(Intent(ctx, RemoteActivity::class.java).putExtra("mode", "gamepad")) } }
            if (state == "ready") {
                MacRow("只需配对一次", "On the computer: Bluetooth settings › Add device (Windows: Bluetooth; Mac: System Settings › Bluetooth; Linux: Bluetooth › +), " +
                    "then pick “${core.store.deviceName}”. Afterwards it connects by itself while this is on") {
                    Secondary("允许电脑发现它") { findable.launch(core.btPad.discoverableIntent()) }
                }
            }
        }
    }
}

// ---- Clipboard ----

@Composable
private fun ClipboardPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val name = them(core)
    val me = core.store.deviceName
    val ctx = LocalContext.current
    PageHead("剪贴板", "在一台复制，在另一台粘贴。")
    if (core.host == null) NotConnected(core, v)
    MacCard {
        DirHead(name, false, me, true)
        SwitchRow("在 $name 上复制的内容会出现在这里", "即时", core.sync.get("clipboard", "in"), first = true) { core.sync.set("clipboard", "in", it) }
    }
    MacCard {
        DirHead(me, true, name, false)
        SwitchRow("在这里复制的内容会发送到 $name", "TwinDesk 打开时生效；有无线调试则始终生效", core.sync.get("clipboard", "out"), first = true) { core.sync.set("clipboard", "out", it) }
        if (core.host != null) MacRow("立即发送剪贴板", "也可用「快捷设置」里的「发送剪贴板」磁贴") { Secondary("发送") { core.clip.sendCurrent(true) } }
    }
    // Pictures both ways, this device's screenshots among them (Clip.kt, Screenshots.kt). Finding
    // new screenshots needs photo access.
    val clip = core.clip
    val shots = core.shots
    val askPhotos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && !shots.canRead) ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        core.changed()
    }
    MacCard("图片") {
        SwitchRow("图片", "双向同步不超过 10 MB 的图片和截图，且需在 $name 上也开启「剪贴板 › 图片」。不支持蓝牙", clip.picturesOn, first = true) { clip.setPictures(it); shots.watch() }
        if (clip.picturesOn && core.host != null && !clip.peerImages) MacRow("在 $name 上已关闭", "在那边打开「剪贴板 › 图片」（需 TwinDesk 2.3 或更高）")
        if (clip.picturesOn) {
            SwitchRow("截图会发送到 $name", "每次在本机截图都会进入 $name 的剪贴板：在那边粘贴即可", shots.on) { shots.setOn(it) }
            if (shots.on && !shots.canRead) MacRow("允许访问照片", "TwinDesk 只检测新的截图") { Primary("允许") { askPhotos.launch(shots.permission) } }
            if (shots.lastSent.isNotEmpty()) MacRow("最近发送的截图", shots.lastSent)
        }
    }
    if (clip.history.isNotEmpty()) MacCard("最近") {
        clip.history.toList().take(12).forEachIndexed { i, item ->
            // A picture of ours isn't kept here: only the computer's copy again.
            val again = item.dir == "in" || item.picture == null
            MacRow(item.text.replace('\n', ' ').take(120), (if (item.dir == "in") "来自 $name" else "发送到 $name") + if (again) " · 点按复制" else "", first = i == 0, onClick = if (again) ({ clip.copyAgain(item) }) else null)
        }
    }
}

// ---- Notifications ----

@Composable
private fun NotificationsPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val name = them(core)
    val me = core.store.deviceName
    val ctx = LocalContext.current
    PageHead("通知", "互相查看通知。")
    if (core.host == null) NotConnected(core, v)
    if (!core.notifs.supported) Note(
        "此版本的 TwinDesk 无法读取这台设备的通知：Android 的 Play Protect 会阻止侧载应用读取通知。" +
            "无线调试配置好后，$name 会为你安装带通知功能的 TwinDesk：在 $name 的「手机」页 ›「安装」。设置和配对都会保留。",
        "warn",
    )
    else if (!core.notifs.hasAccess) Note("请允许通知使用权，这样本机的通知就能发送到 $name。", "warn")
    MacCard {
        DirHead(me, true, name, false)
        SwitchRow("在 $name 上显示本机的通知", "${core.notifs.sent} sent", core.sync.get("notifs", "out"), first = true) { core.sync.set("notifs", "out", it) }
        SwitchRow("隐藏消息内容", "只把应用名和标题发送到 $name", core.store.flag("notifs.hideText", false)) { core.store.setFlag("notifs.hideText", it); core.changed() }
        if (core.notifs.supported && !core.notifs.hasAccess) MacRow("通知使用权", "读取通知需要它") { Primary("允许") { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
        if (!core.notifs.supported && !core.adb.paired) MacRow("无线调试", "请先完成设置，然后从 $name 安装") { Primary("指南") { Guide.open.value = true } }
    }
    MacCard {
        DirHead(name, false, me, true)
        SwitchRow("在此显示 $name 的通知", "${core.notifs.shown} shown", core.sync.get("notifs", "in"), first = true) { core.sync.set("notifs", "in", it) }
    }
}

// ---- Files ----

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val host = core.host
    val ctx = LocalContext.current
    val name = them(core)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) core.files.send(core.files.describe(uris), "send") }
    val pickCopy = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) core.fclip.copy(core.files.describe(uris)) }
    PageHead("文件", "发送文件、复制粘贴、拖放传输。")
    if (host == null) return NotConnected(core, v)

    core.fclip.incoming?.let { o ->
        MacCard("在 $name 上复制") {
            MacRow(if (o.names.size == 1) o.names[0] else "${o.names.size} items", FileClip.size(o.bytes), first = true) {
                Primary("复制") { core.fclip.fetch(o.id, save = false) }
                Secondary("保存") { core.fclip.fetch(o.id, save = true) }
            }
        }
    }

    MacCard("发送到 $name") {
        MacRow("发送文件", "文件会保存到 $name 的「下载」中。在任意应用中选择：分享 › $name", first = true) { Primary("选择…") { pick.launch("*/*") } }
        MacRow("复制文件", "在 $name 上按 Ctrl+Alt+V，粘贴到想要的位置") { Secondary("选择…") { pickCopy.launch("*/*") } }
        // Drag files here from another app (split screen or a floating window).
        var over by remember { mutableStateOf(false) }
        val target = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) { over = true }
                override fun onExited(event: DragAndDropEvent) { over = false }
                override fun onEnded(event: DragAndDropEvent) { over = false }
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    over = false
                    val e = event.toAndroidDragEvent()
                    (ctx as? android.app.Activity)?.requestDragAndDropPermissions(e)
                    val clip = e.clipData ?: return false
                    val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
                    if (uris.isNotEmpty()) core.bg.execute { core.files.send(core.files.describe(uris), "send") }
                    else clip.getItemAt(0)?.text?.toString()?.let { core.clip.sendText(it) }
                    return true
                }
            }
        }
        Box(
            Modifier.padding(16.dp).fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp))
                .border(2.dp, if (over) pal.accent else pal.faint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .background(if (over) pal.accentSoft else Color.Transparent)
                .dragAndDropTarget(shouldStartDragAndDrop = { ev -> ev.mimeTypes().isNotEmpty() }, target = target),
            contentAlignment = Alignment.Center,
        ) { Text(if (over) "松开即发送到 $name" else "把文件拖到这里发送到 $name", color = pal.muted) }
    }
    MacCard("来自 $name") {
        MacRow("拖过屏幕边缘", "拖到这台设备上的文件会存入「下载 › TwinDesk」", first = true)
        MacRow("在 $name 上按 Ctrl+Alt+C", "这台设备会提示复制到剪贴板或保存")
        MacRow("拖放到手机窗口", "拖到 $name 上本机窗口的文件会保存到这里")
    }

    val moving = core.files.sending.map { "Sending ${it.name}" to Files.pct(it.sent, it.bytes) } +
        core.files.receiving.map { "Receiving ${Files.plural(it.count, "file")}" to Files.pct(it.got, it.bytes) }
    if (moving.isNotEmpty() || core.fclip.status.isNotEmpty()) MacCard("当前") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((label, pct) in moving) {
                Text("$label · $pct%", color = pal.text)
                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth(), color = pal.accent, trackColor = pal.trackOff)
            }
            if (core.fclip.status.isNotEmpty()) Text(core.fclip.status, color = pal.muted)
        }
    }
    if (core.files.recent.isNotEmpty()) MacCard("最近") {
        core.files.recent.take(10).forEachIndexed { i, r -> MacRow(r, "", first = i == 0) }
        MacRow("下载", "") { Secondary("显示") { ctx.startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}

// ---- Sound ----

@Composable
private fun SoundPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val name = them(core)
    val me = core.store.deviceName
    PageHead("声音", "在一台设备上听另一台的声音。")
    if (core.host == null) NotConnected(core, v)
    else WifiGate(core, "声音")
    MacCard {
        DirHead(name, false, me, true)
        SwitchRow("在此播放 $name 的声音", if (core.macSound.playing) "播放中" else "", core.sync.get("audio", "in"), first = true) { core.sync.set("audio", "in", it) }
    }
    MacCard {
        DirHead(me, true, name, false)
        SwitchRow("在 $name 上播放本机的声音", soundHint(core), core.sync.get("audio", "out"), first = true) { core.sync.set("audio", "out", it) }
        SoundStart(core)
    }
    Note("随屏幕而来的声音在屏幕视图中播放（「屏幕 › 播放」）。")
}

// This device's sound there: through wireless debugging, else captured here (Sound.kt).
private fun soundHint(core: Core) = when {
    core.sound.on -> "正在那边播放（在此捕获：可在通知中停止）"
    core.sound.error.isNotEmpty() -> core.sound.error
    else -> "经无线调试传输，否则由 TwinDesk 捕获（每次需点「开始」）"
}

// The computer is waiting for the capture: Start here, as in the notification.
@Composable
private fun SoundStart(core: Core) {
    val ctx = LocalContext.current
    if (core.sound.wanted && !core.sound.on) MacRow("开始声音传输", "Android 会请求允许捕获") { Primary("开始") { core.sound.start(ctx) } }
    else if (core.sound.on) MacRow("捕获的声音", "同时在此播放：只有无线调试能把声音从手机移走") { Secondary("停止") { core.sound.stop() } }
}

// ---- General ----

@Composable
private fun GeneralPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    val ctx = LocalContext.current
    val name = them(core)
    var dev by remember { mutableStateOf(core.store.deviceName) }
    PageHead("常规", "")
    MacCard("这台设备") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("名称", color = pal.text, fontSize = 15.sp)
            Text("电脑们如何称呼这台设备", color = pal.muted, fontSize = 12.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(dev, { dev = it }, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = pal.text), shape = RoundedCornerShape(8.dp))
                Secondary("保存") { core.store.setDeviceName(dev); core.changed() }
            }
        }
    }
    val pm = ctx.getSystemService(PowerManager::class.java)
    @Composable
    fun Setup(label: String, hint: String, done: Boolean, action: String, first: Boolean = false, onClick: () -> Unit) =
        MacRow(label, hint, first) { if (done) Pill("已允许", "good") else Primary(action, onClick) }
    // Bluetooth and Wi-Fi Direct, for a computer not on this network.
    val nearbyPerms = (Ble.permissions() + BleServe.permissions() + Direct.permission()).distinct().toTypedArray()
    val nearbyAllowed = nearbyPerms.all { ctx.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
    val askNearby = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) { core.changed() }
    val askNotify = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { core.changed() }
    // Every permission and feature in one place: each is asked for only when switched on elsewhere,
    // or here.
    MacCard("允许") {
        if (Build.VERSION.SDK_INT >= 33) Setup("通知", "文件接收、来自 $name 的邀请，以及 TwinDesk 保持连接",
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED, "允许") {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Calls, Messages, Photos and Files on $name (Hub): what each needs, asked here too.
        for ((what, hint) in listOf(
            "calls" to "在 $name 上接听和拨打电话",
            "messages" to "在 $name 上读取和发送短信",
            "media" to "在 $name 上查看照片和视频",
            "files" to "在 $name 上访问本机存储和共享文件夹，无需 ADB",
        )) {
            val perm = core.hub.perm(what)
            if (perm == "none") MacRow(Hub.label(what), if (what == "files") "需要 Android 11 或更高版本" else "需要带通知功能的 TwinDesk（$name 会安装它）")
            else Setup(Hub.label(what), if (perm == "partial") "部分权限未授予：$hint" else hint, perm == "ok", "允许") { core.hub.askHere(what) }
        }
        if (core.notifs.supported) Setup("通知使用权", "在 $name 上显示本机的通知", core.notifs.hasAccess, "允许") {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else MacRow("通知使用权", "由带通知功能的 TwinDesk 提供，$name 会安装（其「手机」页）")
        Setup("在后台运行", "这样 $name 可随时连接这台设备", pm.isIgnoringBatteryOptimizations(ctx.packageName), "允许") {
            @Suppress("BatteryLife")
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}")))
        }
        MacRow(
            "无线调试",
            when {
                core.adb.paired -> "已与 $name 配对：屏幕和声音在那边，鼠标键盘在这边"
                core.link.ble -> "仅 Wi-Fi 可用：当前是蓝牙连接"
                core.adb.wifiDebugging && !core.adb.setupNeeded -> "已与 $name 配对，正在重新连接…"
                core.adb.wifiDebugging -> "已开启。与 $name 配对一次即可（向导会引导你）"
                else -> "让屏幕和声音显示在 $name，并在此使用那边的鼠标键盘"
            },
        ) {
            if (core.adb.paired) Pill("已配对", "good")
            else if (core.link.ble) Pill("仅 Wi-Fi", "muted")
            else {
                if (!core.adb.wifiDebugging) Secondary("开启") { core.adb.turnOn() }
                Primary("指南") { Guide.open.value = true }
            }
        }
        // Off until switched on (Adb.autoOffEnabled): only what TwinDesk switched on goes off again.
        SwitchRow(
            "断开时关闭无线调试",
            when {
                !core.adb.canEnable -> "Once paired with $name: off again 2 minutes after it disconnects, when TwinDesk switched it on"
                core.adb.autoOffEnabled -> "2 minutes after $name disconnects, when TwinDesk switched it on, and on again as it connects. Switched on by you, it stays on"
                else -> "$name 断开后无线调试保持开启"
            },
            core.adb.autoOffEnabled, enabled = core.adb.canEnable,
        ) { core.adb.setAutoOff(it) }
        Setup("附近的设备", "Bluetooth and Wi-Fi Direct, for $name when it isn’t on this network", nearbyAllowed, "允许") {
            askNearby.launch(nearbyPerms)
        }
        if (core.input.supported) Setup("鼠标和键盘", "Accessibility: $name’s cursor on this phone without wireless debugging, on any network. Only needed when ADB isn’t used", core.input.on, "允许") { core.input.openSettings() }
        else MacRow("鼠标和键盘", "通过无线调试。若不可用：带通知功能的 TwinDesk 自带该能力，$name 会安装（其「手机」页）")
        Setup("显示指针", "在其他应用之上显示 $name 的指针", core.pointer.allowed, "允许") {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }
        if (Build.VERSION.SDK_INT >= 30) Setup("所有文件访问", "把壁纸显示到 $name、文件与共享文件夹", android.os.Environment.isExternalStorageManager(), "允许") {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }
    }
    MacCard("不在同一网络") {
        BleSwitch(core, "请在上方允许「附近的设备」", first = true)
        SwitchRow("Wi-Fi 连接时关闭蓝牙", if (core.bleWifiOff) "当与 $name 在同一 Wi-Fi（非 Wi-Fi Direct）时，蓝牙会关闭直到连接结束"
            else "蓝牙与 Wi-Fi 并存，作为 Wi-Fi 掉线时的退路", core.bleWifiOff) { core.store.setFlag("bleWifiOff", it); core.changed() }
        val n = core.net
        if (n.state != "idle") MacRow("Wi-Fi", when (n.state) {
            "asking", "hosting" -> "正在建立 Wi-Fi 直连…"
            "joining" -> "正在加入 $name 的 Wi-Fi…"
            "joined" -> "已与 $name 建立 Wi-Fi 直连"
            "failed" -> n.error
            else -> ""
        })
    }
    if (core.hosts().isNotEmpty()) MacCard("已配对") {
        core.hosts().forEachIndexed { i, h ->
            MacRow(h.name, if (core.host?.id == h.id) (if (core.link.wired) "已通过 USB 连接" else if (core.link.ble) "已通过蓝牙连接" else "已连接") else "Last address ${h.address}", first = i == 0) {
                if (core.host?.id == h.id) Pill("在线", "good")
                TextButton(onClick = { core.forget(h.id) }) { Text("取消配对", color = pal.bad) }
            }
        }
    }
}

// ---- About ----

@Composable
private fun AboutPage(core: Core, v: Int) {
    @Suppress("UNUSED_EXPRESSION") v // redraw on every Core change (Compose skips unread parameters)
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Logo(110)
        Text("TwinDesk", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.padding(top = 10.dp))
        Text("Version ${BuildConfig.VERSION_NAME}", color = pal.muted)
        Text("让电脑和这台设备如同同一张桌面。", color = pal.muted, modifier = Modifier.padding(top = 6.dp, bottom = 20.dp))
    }
    val ctx = LocalContext.current
    val open = { url: String -> ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    val u = core.updates
    MacCard("更新") {
        MacRow(
            "TwinDesk",
            when {
                u.checking -> "Checking…"
                u.error.isNotEmpty() -> "Couldn’t check: ${u.error}"
                u.available -> "${u.latest} is available. You have ${u.current}. " + (u.download?.let { "Download ${it.name}" } ?: "打开发布页面")
                u.noReleases -> "Version ${u.current}. No releases published yet"
                u.checkedAt > 0 -> "已是最新 · 检查于 " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(u.checkedAt))
                else -> "Version ${u.current}"
            },
            first = true,
        ) {
            if (u.available) Primary("下载") { u.open() }
            if (!u.checking) Secondary("立即检查") { u.checkNow() }
        }
        SwitchRow("自动检查", "每隔几小时在 GitHub 上检查新版本", u.auto) { u.auto = it }
    }
    MacCard("开发者") {
        MacRow("Inuka Asith", "inukaasith.vercel.app", first = true) { Secondary("个人主页") { open("https://inukaasith.vercel.app") } }
        MacRow("源代码", "github.com/InukaAsith/TwinDesk") { Secondary("GitHub") { open("https://github.com/InukaAsith/TwinDesk") } }
        MacRow("全部版本", "各平台的新版本") { Secondary("打开") { open("https://github.com/InukaAsith/TwinDesk/releases") } }
    }
    MacCard("许可证") {
        MacRow("GNU 通用公共许可证 v3", "自由开源软件。版权所有 © 2026 inukaasith", first = true) {
            Secondary("已读") { open("https://github.com/InukaAsith/TwinDesk/blob/main/LICENSE") }
        }
        MacRow("通过无线调试共享屏幕、输入和声音", "scrcpy-server (Apache-2.0)")
        MacRow("网络", "OkHttp (Apache-2.0)")
    }
}
