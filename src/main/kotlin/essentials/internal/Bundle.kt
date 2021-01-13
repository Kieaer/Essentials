package essentials.internal

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
}