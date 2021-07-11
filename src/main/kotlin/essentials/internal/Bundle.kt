package essentials.internal

import essentials.PlayerData
import essentials.data.Config
import essentials.external.UTF8Control
import java.text.MessageFormat
import java.util.*

class Bundle {
    var resource: ResourceBundle
    private val bundleName = "bundle.bundle"

    constructor() {
        resource = when(Config.locale) {
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru", "RU") -> ResourceBundle.getBundle(bundleName, Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle(bundleName, Locale.ENGLISH, UTF8Control())
        }
    }

    constructor(locale: Locale) {
        resource = when(locale) {
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru", "RU") -> ResourceBundle.getBundle(bundleName, Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle(bundleName, Locale.ENGLISH, UTF8Control())
        }
    }

    constructor(playerData: PlayerData?) {
        val locale = if(playerData != null) Locale(playerData.countryCode) else Config.locale
        resource = when(locale) {
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru", "RU") -> ResourceBundle.getBundle(bundleName, Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle(bundleName, Locale.ENGLISH, UTF8Control())
        }
    }

    operator fun get(key: String, vararg parameter: String): String {
        return try {
            MessageFormat.format(resource.getString(key), *parameter)
        } catch(e: MissingResourceException) {
            key
        }
    }
}