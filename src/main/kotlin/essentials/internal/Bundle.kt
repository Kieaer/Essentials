package essentials.internal

import essentials.PlayerData
import essentials.data.Config
import essentials.external.UTF8Control
import java.text.MessageFormat
import java.util.*

class Bundle {
    var resource: ResourceBundle

    constructor() {
        resource = try {
            ResourceBundle.getBundle("bundle.bundle", Config.locale, UTF8Control())
        } catch (e: Exception) {
            ResourceBundle.getBundle("bundle.bundle", Locale.US, UTF8Control())
        }
    }

    constructor(locale: Locale) {
        resource = try {
            ResourceBundle.getBundle("bundle.bundle", locale, UTF8Control())
        } catch (e: Exception) {
            ResourceBundle.getBundle("bundle.bundle", Locale.US, UTF8Control())
        }
    }

    constructor(playerData: PlayerData?) {
        val locale = if(playerData != null) Locale(playerData.countryCode) else Config.locale
        resource = try {
            ResourceBundle.getBundle("bundle.bundle", locale, UTF8Control())
        } catch (e: Exception) {
            ResourceBundle.getBundle("bundle.bundle", Locale.US, UTF8Control())
        }
    }

    operator fun get(key: String, vararg params: String?): String {
        return try {
            MessageFormat.format(resource.getString(key), *params)
        } catch (e: MissingResourceException) {
            key
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

    operator fun get(key: String, vararg parameter: Array<out String?>): String {
        return try {
            MessageFormat.format(Config.prefix + resource.getString(key), *parameter)
        } catch (e: MissingResourceException) {
            key
        }
    }
}