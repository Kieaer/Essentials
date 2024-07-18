package essential.core

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource: ResourceBundle
    var prefix: String = ""
    var locale: Locale = Locale.ENGLISH

    constructor() {
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    constructor(languageTag: String) {
        locale = try {
            when (languageTag.substring(0, 2)) {
                "ko" -> Locale.KOREA
                else -> Locale.ENGLISH
            }
        } catch (e: Exception) {
            Locale.ENGLISH
        }
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    operator fun get(key: String): String {
        return if (prefix.isEmpty()) {
            MessageFormat.format(resource.getString(key))
        } else {
            "$prefix " + MessageFormat.format(resource.getString(key))
        }
    }

    operator fun get(key: String, vararg parameter: Any): String {
        return if (prefix.isEmpty()) {
            MessageFormat.format(resource.getString(key), *parameter)
        } else {
            "$prefix " + MessageFormat.format(resource.getString(key), *parameter)
        }
    }
}