package com.artemiy.player.playback

import android.content.Context
import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
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

enum class OutputKind { PHONE, HEADPHONES, EARBUDS, SPEAKER, BLUETOOTH, USB, TV, CAR }

/** Where the music is coming out right now — [name] is null for the phone's own speaker. */
data class OutputDevice(val kind: OutputKind, val name: String?, val isBluetooth: Boolean = false)

private val PHONE = OutputDevice(OutputKind.PHONE, null)

/**
 * Tracks the current media output, updating as things are plugged in, paired or switched.
 * [active]: the screen showing it is actually visible — only then does it keep re-checking, and
 * only then, the first time a Bluetooth device turns up, does it ask for "Nearby devices" (needed
 * to read the name you gave the device rather than its factory one).
 */
@Composable
fun rememberOutputDevice(active: Boolean = true): OutputDevice {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var device by remember { mutableStateOf(currentOutput(audio, context)) }
    DisposableEffect(audio) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { device = currentOutput(audio, context) }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { device = currentOutput(audio, context) }
        }
        audio.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { audio.unregisterAudioDeviceCallback(callback) }
    }
    // Switching between already-connected devices (from the system output picker) doesn't add or
    // remove anything, so there's no callback for it — a cheap re-check every couple of seconds.
    LaunchedEffect(audio, active) {
        while (active) {
            device = currentOutput(audio, context)
            delay(1500)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        device = currentOutput(audio, context)
    }
    var askedForNearby by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(device.isBluetooth, active) {
        if (active && device.isBluetooth && !askedForNearby && Build.VERSION.SDK_INT >= 31 && !hasBluetoothConnect(context)) {
            askedForNearby = true
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
    return device
}

private fun hasBluetoothConnect(context: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

/**
 * What the Bluetooth device itself says about itself, when Android lets us ask ("Nearby devices"):
 * the name the user gave it, and what kind of thing it is — headphones, an in-ear headset
 * (earbuds report themselves as that), a speaker, a car... Null parts = unknown.
 */
private class BluetoothDetails(val alias: String?, val kind: OutputKind?)

private fun bluetoothDetails(context: Context, info: AudioDeviceInfo): BluetoothDetails? {
    if (Build.VERSION.SDK_INT < 30 || !hasBluetoothConnect(context)) return null
    val address = info.address.takeIf { it.isNotBlank() } ?: return null
    val device = runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.getRemoteDevice(address) }
        .getOrNull() ?: return null
    val alias = runCatching { device.alias }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    val kind = when (runCatching { device.bluetoothClass?.deviceClass }.getOrNull()) {
        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> OutputKind.HEADPHONES
        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
        BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> OutputKind.EARBUDS
        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
        BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
        BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> OutputKind.SPEAKER
        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> OutputKind.CAR
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
        BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> OutputKind.TV
        else -> null
    }
    return BluetoothDetails(alias, kind)
}

private val MEDIA_ATTRIBUTES = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()

private fun currentOutput(audio: AudioManager, context: Context): OutputDevice {
    // Android 13+ can say exactly where media is routed — but on some phones that call wants a
    // system permission an ordinary app doesn't have, and just fails. Then (and on older Android)
    // guess the way Android itself usually picks: the most "personal" connected output wins over
    // the phone's own speaker.
    val routed = if (Build.VERSION.SDK_INT >= 33) {
        runCatching { audio.getAudioDevicesForAttributes(MEDIA_ATTRIBUTES).firstOrNull() }
            .onFailure { android.util.Log.d("OutputDevice", "Routed device unavailable: ${it.javaClass.simpleName}") }
            .getOrNull()
    } else null
    val info = routed
        ?: runCatching { audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).maxByOrNull { priorityOf(it.type) } }.getOrNull()
        ?: return PHONE
    val described = describe(info)
    if (!described.isBluetooth) return described
    val details = bluetoothDetails(context, info) ?: return described
    return described.copy(
        name = details.alias ?: described.name,
        kind = details.kind ?: described.kind,
    )
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
        AudioDeviceInfo.TYPE_BLE_HEADSET -> OutputDevice(OutputKind.HEADPHONES, name ?: "Bluetooth", isBluetooth = true)
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> OutputDevice(OutputKind.SPEAKER, name ?: "Bluetooth", isBluetooth = true)
        // Bluetooth doesn't say what kind of thing it is without extra permissions, so the name
        // is the best hint there is.
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_BROADCAST ->
            OutputDevice(bluetoothKindFromName(name), name ?: "Bluetooth", isBluetooth = true)
        else -> OutputDevice(OutputKind.SPEAKER, name ?: "Внешнее устройство")
    }
}

private val EARBUD_HINTS = listOf("bud", "pod", "ear", "freeclip", "wf-", "капл")
private val HEADPHONE_HINTS = listOf("head", "наушн", "wh-", "major", "momentum", "qc")
private val SPEAKER_HINTS = listOf("speaker", "колонк", "boom", "flip", "charge", "jbl", "sound", "station", "станция", "алиса", "home")

/** For devices that don't say what they are: a guess from the name. */
private fun bluetoothKindFromName(name: String?): OutputKind {
    val lower = name?.lowercase() ?: return OutputKind.BLUETOOTH
    return when {
        EARBUD_HINTS.any { it in lower } -> OutputKind.EARBUDS
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
