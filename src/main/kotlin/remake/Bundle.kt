package remake

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource: ResourceBundle

    constructor() {
        resource = ResourceBundle.getBundle("bundle", Locale.ENGLISH)
    }

    constructor(locale: Locale) {
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    operator fun get(key: String): String {
        return MessageFormat.format(resource.getString(key))
    }

    operator fun get(key: String, vararg parameter: String): String {
        return MessageFormat.format(resource.getString(key), *parameter)
    }
}