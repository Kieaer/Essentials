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
        resource = ResourceBundle.getBundle(bundleName, Config.locale, UTF8Control())
    }

    constructor(locale: Locale) {
        resource = ResourceBundle.getBundle(bundleName, locale, UTF8Control())
    }

    constructor(playerData: PlayerData?) {
        val locale = if(playerData != null) Locale(playerData.countryCode) else Config.locale
        resource = ResourceBundle.getBundle(bundleName, locale, UTF8Control())
    }

    operator fun get(key: String, vararg parameter: String): String {
        return try {
            MessageFormat.format(resource.getString(key), *parameter)
        } catch(e: MissingResourceException) {
            key
        }
    }
}