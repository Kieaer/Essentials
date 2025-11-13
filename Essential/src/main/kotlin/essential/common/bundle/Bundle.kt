package essential.common.bundle

import java.text.MessageFormat
import java.util.*

class Bundle {
    val resource: ResourceBundle
    var prefix: String = ""
    var locale: Locale = Locale.getDefault()

    constructor() {
        resource = ResourceBundle.getBundle("bundles/common/bundle", locale)
    }

    constructor(source: ResourceBundle) {
        resource = source
    }

    constructor(languageTag: String) {
        locale = Locale.forLanguageTag(languageTag)
        resource = ResourceBundle.getBundle("bundles/common/bundle", locale)
    }

    constructor(languageTag: String, source: ResourceBundle) {
        locale = Locale.forLanguageTag(languageTag)
        resource = source
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
