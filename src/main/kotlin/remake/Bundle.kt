package remake

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource: ResourceBundle

    init {
        Locale.setDefault(Locale.ENGLISH)
    }

    constructor() {
        resource = ResourceBundle.getBundle("bundle")
    }

    constructor(languageTag: String) {
        val locale = when (languageTag.substring(0,2)) {
            "ko" -> Locale.KOREA
            else -> Locale.ENGLISH
        }
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    operator fun get(key: String): String {
        return MessageFormat.format(resource.getString(key))
    }

    operator fun get(key: String, vararg parameter: String): String {
        return MessageFormat.format(resource.getString(key), *parameter)
    }
}