package com.artemiy.player.ui.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * The language the app's own texts are in: the phone's (English when the phone's language isn't
 * one the app has), or one picked in the app. Each is named in itself, so it's findable whatever
 * language the app is showing. An [expressive] one says the same things livelier and friendlier;
 * its texts live under the private-use region "XE" and fall back to the plain ones.
 */
enum class AppLanguage(val tag: String, val nativeName: String, val expressive: Boolean = false) {
    RUSSIAN("ru", "Русский"),
    RUSSIAN_EXPRESSIVE("ru-XE", "Русский", expressive = true),
    /** Pre-reform spelling (до 1918): hard signs, yat and all — generated from the Russian texts
     * (tools/i18n/oldrus.py) under the private-use region "XP". */
    RUSSIAN_OLD("ru-XP", "Русскій дореформенный"),
    ENGLISH("en", "English"),
    ENGLISH_EXPRESSIVE("en-XE", "English", expressive = true),
    UKRAINIAN("uk", "Українська"),
    POLISH("pl", "Polski"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt-BR", "Português (Brasil)"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano"),
    TURKISH("tr", "Türkçe"),
    JAPANESE("ja", "日本語"),
    CHINESE("zh-CN", "中文（简体）"),
    KOREAN("ko", "한국어"),
}

/**
 * Kept in plain SharedPreferences rather than the settings DataStore: it has to be known
 * synchronously, before the screen is even created (see [withAppLanguage]).
 */
object LanguagePrefs {
    private const val FILE = "app_language"
    private const val KEY = "language"

    /** The picked language, or null while none has been picked (then the phone's is used). */
    fun get(context: Context): AppLanguage? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }

    /** The language the app is actually in: the picked one, or the one matching the phone's
     * (English when the app doesn't have the phone's language). */
    fun effective(context: Context): AppLanguage = get(context) ?: run {
        val phone = android.content.res.Resources.getSystem().configuration.locales[0].language
        AppLanguage.entries.firstOrNull { !it.expressive && it.tag.substringBefore('-') == phone } ?: AppLanguage.ENGLISH
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, language.name).commit()
    }
}

/** [this] context with the app's chosen language applied (unchanged when it follows the phone). */
fun Context.withAppLanguage(): Context {
    val tag = LanguagePrefs.get(this)?.tag ?: return this
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}

/** [this] context showing its resources in [language] — for a label in a language other than the
 * app's current one (the language picker writes each language's words in that language). */
fun Context.inLanguage(language: AppLanguage): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Locale.forLanguageTag(language.tag))
    return createConfigurationContext(config)
}
