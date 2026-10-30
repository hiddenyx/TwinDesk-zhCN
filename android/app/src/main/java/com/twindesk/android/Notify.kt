// TwinDesk — Copyright (C) 2026 inukaasith. GPL-3.0, see LICENSE.
// Every notification TwinDesk posts: the link (the foreground service), invites, files arriving,
// files copied on the computer (Copy / Save), and the computer's own notifications.
package com.twindesk.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.content.Intent
import android.net.Uri

object Notify {
    const val CH_LINK = "link"
    const val CH_INVITE = "invite"
    const val CH_FILES = "files"
    const val CH_CLIP = "clip"
    const val CH_COMPUTER = "computer"
    const val CH_UPDATES = "updates"
    const val CH_SCREEN = "screen"
    const val CH_ASK = "ask"

    const val ID_LINK = 1
    const val ID_INVITE = 2
    const val ID_FCLIP = 3
    const val ID_ADB = 4
    const val ID_PAIR = 5
    const val ID_UPDATE = 6
    const val ID_SCREEN = 7
    const val ID_HOTSPOT = 8
    const val ID_WIFI = 9
    const val ID_INPUT = 10
    const val ID_HUB_ASK = 11
    const val ID_SOUND = 12 // the sound capture: asking for Start, then its service's own
    private const val ID_FILES_BASE = 1000
    private const val ID_COMPUTER_BASE = 100000

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun channels(ctx: Context) {
        val n = nm(ctx)
        n.createNotificationChannel(NotificationChannel(CH_LINK, "连接", NotificationManager.IMPORTANCE_LOW).apply {
            description = "TwinDesk 等待电脑连接时显示"
            setShowBadge(false)
        })
        n.createNotificationChannel(NotificationChannel(CH_INVITE, "配对请求", NotificationManager.IMPORTANCE_HIGH))
        n.createNotificationChannel(NotificationChannel(CH_FILES, "文件", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "电脑发来的文件"
        })
        n.createNotificationChannel(NotificationChannel(CH_CLIP, "电脑上已复制", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "用快捷键复制的文件，可在此复制或保存"
        })
        n.createNotificationChannel(NotificationChannel(CH_COMPUTER, "电脑的通知", NotificationManager.IMPORTANCE_HIGH))
        n.createNotificationChannel(NotificationChannel(CH_SCREEN, "共享的屏幕", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "电脑已开始与这台手机共享屏幕"
        })
        n.createNotificationChannel(NotificationChannel(CH_UPDATES, "更新", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "GitHub 上有 TwinDesk 新版本"
        })
    }

    fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun action(ctx: Context, action: String, req: Int, extra: String = ""): PendingIntent = PendingIntent.getBroadcast(
        ctx, req, Intent(ctx, ActionReceiver::class.java).setAction(action).putExtra("arg", extra),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // The computer started sharing and the viewer couldn't open by itself (no "Display over other
    // apps"): tap to see it.
    fun screen(ctx: Context, name: String) {
        val open = PendingIntent.getActivity(
            ctx, 7, Intent(ctx, ViewerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        runCatching {
            nm(ctx).notify(ID_SCREEN, Notification.Builder(ctx, CH_SCREEN)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("$name 正在共享屏幕")
                .setContentText("点按在此查看")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
    }

    fun cancelScreen(ctx: Context) = runCatching { nm(ctx).cancel(ID_SCREEN) }.let { }

    fun link(ctx: Context, core: Core): Notification {
        val h = core.host
        val b = Notification.Builder(ctx, CH_LINK)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentIntent(openApp(ctx))
            .setOngoing(true)
            .setShowWhen(false)
        when {
            h != null -> {
                b.setContentTitle("Connected to ${h.name}")
                b.setContentText(core.files.activeLine() ?: "文件、剪贴板和通知已共享")
                // Three buttons at most: wireless debugging being off has its own notification (adbOff).
                b.addAction(Notification.Action.Builder(null, "发送剪贴板", action(ctx, ActionReceiver.SEND_CLIP, 11)).build())
                b.addAction(Notification.Action.Builder(null, "断开连接", action(ctx, ActionReceiver.DISCONNECT, 12)).build())
            }
            core.link.state == "connecting" -> b.setContentTitle("Connecting to ${core.link.host?.name ?: "your computer"}…")
            else -> {
                b.setContentTitle("已就绪，等待电脑连接")
                val near = core.hosts().firstOrNull { core.isNear(it.id) }
                b.setContentText(if (near != null) "${near.name} is nearby" else "在电脑上打开 TwinDesk 以连接")
                if (near != null) b.addAction(Notification.Action.Builder(null, "连接到 ${near.name}", action(ctx, ActionReceiver.CONNECT, 13, near.id)).build())
            }
        }
        b.addAction(Notification.Action.Builder(null, "退出", action(ctx, ActionReceiver.EXIT, 15)).build())
        return b.build()
    }

    fun invite(ctx: Context, name: String) {
        nm(ctx).notify(
            ID_INVITE,
            Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("$name 请求配对")
                .setContentText("打开 TwinDesk 核对配对码")
                .setContentIntent(openApp(ctx))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelInvite(ctx: Context) = nm(ctx).cancel(ID_INVITE)

    // The computer (a Mac, through Bluetooth) wants this phone's screen, which needs Wi-Fi: the
    // hotspot, which only the person can switch on. Tap: the hotspot settings.
    fun hotspot(ctx: Context, name: String) {
        val settings = Intent("android.settings.TETHER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val target = if (settings.resolveActivity(ctx.packageManager) != null) settings
            else Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(ctx, 8, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching {
            nm(ctx).notify(ID_HOTSPOT, Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("为 $name 开启热点")
                .setContentText("$name 不在这台手机的网络中。点按打开热点，然后在 $name 上输入热点名称和密码。")
                .setStyle(Notification.BigTextStyle().bigText("$name 不在这台手机的网络中。点按打开热点，然后在 $name 上输入热点名称和密码。"))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
    }

    fun cancelHotspot(ctx: Context) = nm(ctx).cancel(ID_HOTSPOT)

    // Wi-Fi Direct needs Wi-Fi on (no network needed), and Android doesn't let apps switch it on.
    fun wifiOff(ctx: Context, name: String) {
        val panel = Intent(if (Build.VERSION.SDK_INT >= 29) android.provider.Settings.Panel.ACTION_WIFI else android.provider.Settings.ACTION_WIFI_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(ctx, 9, panel, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "Files and screens with $name go over a direct Wi-Fi link. Tap to switch Wi-Fi on; it doesn’t need to join a network."
        runCatching {
            nm(ctx).notify(ID_WIFI, Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("为 $name 开启 Wi-Fi")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
        // TwinDesk on screen: the Wi-Fi panel straight away (Android ignores this from the background).
        runCatching { ctx.startActivity(panel) }
    }

    fun cancelWifiOff(ctx: Context) = nm(ctx).cancel(ID_WIFI)

    // The computer's mouse wants this phone without wireless debugging: the Accessibility switch.
    // supported: this build has the Accessibility service (the full one); else the app, which says how to get it.
    fun input(ctx: Context, name: String, supported: Boolean) {
        val to = if (supported) Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS) else Intent(ctx, MainActivity::class.java)
        val open = PendingIntent.getActivity(ctx, 10, to.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = if (supported) "要在没有无线调试的情况下在此使用 $name 的鼠标和键盘，请在辅助功能（已安装的应用）中开启 TwinDesk。点按打开。"
            else "此版本的 TwinDesk 无法在没有无线调试的情况下接收 $name 的鼠标和键盘。请先配置无线调试，然后 $name 会安装带通知功能的 TwinDesk（其「手机」页 ›「安装」），即可使用。"
        runCatching {
            nm(ctx).notify(ID_INPUT, Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Let $name control this phone")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
    }

    fun cancelInput(ctx: Context) = nm(ctx).cancel(ID_INPUT)

    // The computer wants this phone's sound and adb can't carry it (Sound.kt): Start asks for the capture.
    fun sound(ctx: Context, name: String) {
        val start = PendingIntent.getActivity(ctx, 12, Intent(ctx, SoundActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "没有无线调试时由 TwinDesk 捕获声音：点「开始」后 Android 会再询问一次。不允许捕获的应用将保持无声。"
        runCatching {
            nm(ctx).notify(ID_SOUND, Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("在 $name 上播放这台手机的声音")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(start)
                .addAction(Notification.Action.Builder(null, "开始", start).build())
                .setOnlyAlertOnce(true)
                .build())
        }
    }

    fun cancelSound(ctx: Context) = runCatching { nm(ctx).cancel(ID_SOUND) }.let { }

    // While it plays there (SoundService's own notification): Stop ends it.
    fun soundOn(ctx: Context, name: String): Notification = Notification.Builder(ctx, CH_LINK)
        .setSmallIcon(R.drawable.ic_stat)
        .setContentTitle("这台手机的声音会在 $name 上播放")
        .setContentIntent(openApp(ctx))
        .setOngoing(true)
        .addAction(Notification.Action.Builder(null, "停止", action(ctx, ActionReceiver.SOUND_STOP, 13)).build())
        .build()

    // The computer's Allow for Calls, Messages or Photos, when the prompt can't open by itself
    // (no "显示在其他应用上层"): tap for Android's prompt. Its channel is made the first time.
    fun hubAsk(ctx: Context, what: String, name: String) {
        val open = PendingIntent.getActivity(ctx, 11, Intent(ctx, PermActivity::class.java).putExtra("what", what).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "$name asks to use ${Hub.label(what)} from this phone. Tap to allow it."
        runCatching {
            nm(ctx).createNotificationChannel(NotificationChannel(CH_ASK, "来自电脑的请求", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "电脑正在申请所需权限（通话、短信、照片）"
            })
            nm(ctx).notify(ID_HUB_ASK, Notification.Builder(ctx, CH_ASK)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Allow ${Hub.label(what)} for $name")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
    }

    fun cancelHubAsk(ctx: Context) = runCatching { nm(ctx).cancel(ID_HUB_ASK) }.let { }

    // A new release on GitHub (Updates): tap to download it in the browser.
    fun update(ctx: Context, latest: String, current: String) {
        val open = PendingIntent.getActivity(
            ctx, 50, Intent(Intent.ACTION_VIEW, Uri.parse(Core.get(ctx).updates.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm(ctx).notify(
            ID_UPDATE,
            Notification.Builder(ctx, CH_UPDATES)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("TwinDesk $latest 已发布")
                .setContentText("当前版本为 $current。点按下载新版本。")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    // Wireless debugging went off while connected (MIUI / HyperOS do that): tap to turn it on.
    fun adbOff(ctx: Context, computer: String) {
        val open = PendingIntent.getActivity(
            ctx, 30, Intent(ctx, AdbActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm(ctx).notify(
            ID_ADB,
            Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("无线调试已关闭")
                .setContentText("$computer 需要它来显示这块屏幕、在此使用鼠标键盘并传输声音。点按后请同时开启 USB 调试和无线调试。")
                .setStyle(Notification.BigTextStyle().bigText("$computer 需要它来显示这块屏幕、在此使用鼠标键盘并传输声音。点按后请同时开启 USB 调试和无线调试。"))
                .setContentIntent(open)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun cancelAdbOff(ctx: Context) = nm(ctx).cancel(ID_ADB)

    // The pairing code box: type the code from "使用配对码配对设备" right here, so
    // Settings' dialog stays open (it closes when you switch apps).
    fun pairCode(ctx: Context, computer: String) {
        val input = android.app.RemoteInput.Builder("code").setLabel("6 位配对码").build()
        val pi = PendingIntent.getBroadcast(
            ctx, 40, Intent(ctx, ActionReceiver::class.java).setAction(ActionReceiver.PAIR_CODE),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm(ctx).notify(
            ID_PAIR,
            Notification.Builder(ctx, CH_INVITE)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("在此输入配对码")
                .setContentText("设置中会显示 6 位配对码。在此输入即可与 $computer 配对。")
                .setStyle(Notification.BigTextStyle().bigText("设置中的「使用配对码配对设备」会显示 6 位配对码。点「输入配对码」，输入后发送。剩下的由 $computer 完成。"))
                .setOnlyAlertOnce(false)
                .addAction(Notification.Action.Builder(null, "输入配对码", pi).addRemoteInput(input).build())
                .build(),
        )
    }

    fun pairResult(ctx: Context, title: String, text: String, again: Boolean, progress: Boolean = false) {
        val b = Notification.Builder(ctx, CH_INVITE)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setAutoCancel(!progress)
        if (progress) b.setProgress(0, 0, true).setOngoing(true)
        if (again) {
            val input = android.app.RemoteInput.Builder("code").setLabel("6 位配对码").build()
            val pi = PendingIntent.getBroadcast(
                ctx, 40, Intent(ctx, ActionReceiver::class.java).setAction(ActionReceiver.PAIR_CODE),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            b.addAction(Notification.Action.Builder(null, "输入配对码", pi).addRemoteInput(input).build())
        }
        nm(ctx).notify(ID_PAIR, b.build())
    }

    // Files on their way (progress) and when they are here (open / show).
    // up: going to the computer (the upload arrow).
    fun files(ctx: Context, key: Int, title: String, text: String, progress: Int?, open: Intent?, up: Boolean = false) {
        val b = Notification.Builder(ctx, CH_FILES)
            .setSmallIcon(if (up) (if (progress != null) android.R.drawable.stat_sys_upload else android.R.drawable.stat_sys_upload_done)
                else if (progress != null) android.R.drawable.stat_sys_download else android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(progress != null)
            .setAutoCancel(progress == null)
        if (progress != null) b.setProgress(100, progress, progress < 0)
        if (open != null) b.setContentIntent(PendingIntent.getActivity(ctx, key, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        nm(ctx).notify(ID_FILES_BASE + key, b.build())
    }

    fun cancelFiles(ctx: Context, key: Int) = nm(ctx).cancel(ID_FILES_BASE + key)

    // The computer copied files with its shortcut: copy them here, or save them.
    fun fclipOffer(ctx: Context, from: String, what: String, id: String) {
        nm(ctx).notify(
            ID_FCLIP,
            Notification.Builder(ctx, CH_CLIP)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("在 $from 上复制")
                .setContentText(what)
                .setContentIntent(openApp(ctx))
                .setTimeoutAfter(10 * 60 * 1000L)
                .addAction(Notification.Action.Builder(null, "复制到剪贴板", action(ctx, ActionReceiver.FCLIP_COPY, 21, id)).build())
                .addAction(Notification.Action.Builder(null, "保存到「下载」", action(ctx, ActionReceiver.FCLIP_SAVE, 22, id)).build())
                .build(),
        )
    }

    // Files are on the clipboard: paste them anywhere, or share them.
    fun fclipReady(ctx: Context, title: String, text: String, share: Intent?) {
        val b = Notification.Builder(ctx, CH_CLIP)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(5 * 60 * 1000L)
            .setAutoCancel(true)
        if (share != null) {
            val pi = PendingIntent.getActivity(ctx, 23, Intent.createChooser(share, "分享").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(Notification.Action.Builder(null, "分享…", pi).build())
            b.setContentIntent(pi)
        }
        nm(ctx).notify(ID_FCLIP, b.build())
    }

    fun fclipProgress(ctx: Context, title: String, progress: Int) {
        nm(ctx).notify(
            ID_FCLIP,
            Notification.Builder(ctx, CH_CLIP)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, progress, progress < 0)
                .build(),
        )
    }

    fun cancelFclip(ctx: Context) = nm(ctx).cancel(ID_FCLIP)

    // icon: the app's (from the computer), else TwinDesk's own shows.
    fun fromComputer(ctx: Context, key: String, app: String, title: String, body: String, computer: String, icon: android.graphics.Bitmap? = null) {
        nm(ctx).notify(
            ID_COMPUTER_BASE + (key.hashCode() and 0xffff),
            Notification.Builder(ctx, CH_COMPUTER)
                .setSmallIcon(R.drawable.ic_stat)
                .setLargeIcon(icon)
                .setSubText("$app · $computer")
                .setContentTitle(title.ifEmpty { app })
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun viewIntent(uri: Uri, mime: String?): Intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
}
