package essentials.internal

import essentials.PlayerData
import essentials.data.Config
import essentials.external.UTF8Control
import java.text.MessageFormat
import java.util.*

class Bundle {
    var resource: ResourceBundle

    constructor() {
        resource = when(Config.locale){
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru","RU") -> ResourceBundle.getBundle("bundle.bundle", Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH, UTF8Control())
        }
    }

    constructor(locale: Locale) {
        resource = when(locale){
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru","RU") -> ResourceBundle.getBundle("bundle.bundle", Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH, UTF8Control())
        }
    }

    constructor(playerData: PlayerData?) {
        val locale = if(playerData != null) Locale(playerData.countryCode) else Config.locale
        resource = when(locale){
            Locale.ENGLISH, Locale.KOREAN, Locale.CHINESE, Locale("ru","RU") -> ResourceBundle.getBundle("bundle.bundle", Config.locale, UTF8Control())
            else -> ResourceBundle.getBundle("bundle.bundle", Locale.ENGLISH, UTF8Control())
        }
    }

    fun prefix(key: String, vararg params: String?): String {
        return try {
            MessageFormat.format(Config.prefix + resource.getString(key), *params)
        } catch (e: MissingResourceException) {
            key
        }
    }

    operator fun get(key: String): String {
        return try {
            MessageFormat.format(resource.getString(key))
        } catch (e: MissingResourceException) {
            key
        }
    }

    operator fun get(key: String, vararg parameter: String): String {
        return try {
            MessageFormat.format(resource.getString(key), *parameter)
        } catch (e: MissingResourceException) {
            key
        }
    }
}