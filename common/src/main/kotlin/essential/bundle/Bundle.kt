package essential.bundle

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource: ResourceBundle
    var prefix: String = ""
    var locale: Locale = Locale.getDefault()

    constructor() {
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    constructor(source: ResourceBundle) {
        resource = source
    }

    constructor(languageTag: String) {
        locale = Locale(languageTag)
        resource = ResourceBundle.getBundle("bundle", locale)
    }

    operator fun get(key: String): String {
        if (!resource.containsKey(key)) return key

        return if (prefix.isEmpty()) {
            MessageFormat.format(resource.getString(key))
        } else {
            "$prefix " + MessageFormat.format(resource.getString(key))
        }
    }

    operator fun get(key: String, vararg parameter: Any): String {
        if (!resource.containsKey(key)) return key

        return if (prefix.isEmpty()) {
            MessageFormat.format(resource.getString(key), *parameter)
        } else {
            "$prefix " + MessageFormat.format(resource.getString(key), *parameter)
        }
    }
}