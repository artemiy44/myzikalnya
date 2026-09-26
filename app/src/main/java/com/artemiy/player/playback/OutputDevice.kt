package com.artemiy.player.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

enum class OutputKind { PHONE, HEADPHONES, SPEAKER, BLUETOOTH, USB, TV }

/** Where the music is coming out right now — [name] is null for the phone's own speaker. */
data class OutputDevice(val kind: OutputKind, val name: String?)

private val PHONE = OutputDevice(OutputKind.PHONE, null)

/** Tracks the current media output, updating as things are plugged in, paired or switched. */
@Composable
fun rememberOutputDevice(): OutputDevice {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var device by remember { mutableStateOf(currentOutput(audio)) }
    DisposableEffect(audio) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { device = currentOutput(audio) }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { device = currentOutput(audio) }
        }
        audio.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { audio.unregisterAudioDeviceCallback(callback) }
    }
    // Switching between already-connected devices (from the system output picker) doesn't add or
    // remove anything, so there's no callback for it — a cheap re-check every couple of seconds.
    LaunchedEffect(audio) {
        while (true) {
            delay(1500)
            device = currentOutput(audio)
        }
    }
    return device
}

private val MEDIA_ATTRIBUTES = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

private fun currentOutput(audio: AudioManager): OutputDevice {
    val info = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            audio.getAudioDevicesForAttributes(MEDIA_ATTRIBUTES).firstOrNull()
        } else {
            // Older Android can't say where media is routed, so guess the way it usually picks:
            // the most "personal" connected output wins over the phone's speaker.
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).maxByOrNull { priorityOf(it.type) }
        }
    }.getOrNull() ?: return PHONE
    return describe(info)
}

private fun priorityOf(type: Int): Int = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> 5
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> 4
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> 3
    AudioDeviceInfo.TYPE_HDMI -> 2
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 1
    else -> 0
}

private fun describe(info: AudioDeviceInfo): OutputDevice {
    val name = info.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != Build.MODEL }
    return when (info.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> PHONE
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET ->
            OutputDevice(OutputKind.HEADPHONES, "Наушники")
        AudioDeviceInfo.TYPE_USB_HEADSET -> OutputDevice(OutputKind.HEADPHONES, name ?: "USB-наушники")
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> OutputDevice(OutputKind.USB, name ?: "USB-аудио")
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC ->
            OutputDevice(OutputKind.TV, name ?: "HDMI")
        AudioDeviceInfo.TYPE_BLE_HEADSET -> OutputDevice(OutputKind.HEADPHONES, name ?: "Bluetooth")
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> OutputDevice(OutputKind.SPEAKER, name ?: "Bluetooth")
        // Bluetooth doesn't say what kind of thing it is without extra permissions, so the name
        // is the best hint there is.
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_BROADCAST ->
            OutputDevice(bluetoothKindFromName(name), name ?: "Bluetooth")
        else -> OutputDevice(OutputKind.SPEAKER, name ?: "Внешнее устройство")
    }
}

private val HEADPHONE_HINTS = listOf("bud", "ear", "pod", "head", "наушн", "wh-", "wf-", "freeclip")
private val SPEAKER_HINTS = listOf("speaker", "колонк", "boom", "flip", "charge", "jbl", "sound", "station", "станция", "алиса", "home")

private fun bluetoothKindFromName(name: String?): OutputKind {
    val lower = name?.lowercase() ?: return OutputKind.BLUETOOTH
    return when {
        HEADPHONE_HINTS.any { it in lower } -> OutputKind.HEADPHONES
        SPEAKER_HINTS.any { it in lower } -> OutputKind.SPEAKER
        else -> OutputKind.BLUETOOTH
    }
}

/**
 * Opens Android's own "where should this play" sheet for this app — the same one as the media
 * player in quick settings. Android 14+ has an official call for it; 11–13 only via SystemUI's
 * broadcast; anything older (or a skin without it) falls back to Bluetooth settings.
 */
fun openOutputSwitcher(context: Context) {
    if (Build.VERSION.SDK_INT >= 34 && runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)) return
    if (Build.VERSION.SDK_INT >= 30) {
        val shown = runCatching {
            context.sendBroadcast(
                Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                    .setPackage("com.android.systemui")
                    .putExtra("com.android.systemui.extra.PACKAGE_NAME", context.packageName),
            )
        }.isSuccess
        if (shown && Build.VERSION.SDK_INT >= 31) return
    }
    runCatching {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
