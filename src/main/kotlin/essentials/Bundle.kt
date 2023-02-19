package essentials

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource : ResourceBundle

    constructor() {
        resource = ResourceBundle.getBundle("bundle", Locale.getDefault())
    }

    constructor(languageTag : String) {
        val locale = try {
            when(languageTag.substring(0, 2)) {
                "ko" -> Locale.KOREA
                else -> Locale.ENGLISH
            }
        } catch(e : Exception) {
            Locale.ENGLISH
        }
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    operator fun get(key : String) : String {
        return MessageFormat.format(resource.getString(key))
    }

    operator fun get(key : String, vararg parameter : Any) : String {
        return MessageFormat.format(resource.getString(key), *parameter)
    }
}