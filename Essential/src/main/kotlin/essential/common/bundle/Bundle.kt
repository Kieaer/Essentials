package essential.common.bundle

import java.text.MessageFormat
import java.util.*

class Bundle {
    var resource: ResourceBundle
    var prefix: String = ""
    var locale: Locale = Locale.getDefault()

    constructor() {
        resource = ResourceBundle.getBundle("bundles/common/bundle", locale)
    }

    constructor(source: ResourceBundle) {
        resource = source
    }

    constructor(baseName: String, locale: Locale) {
        this.locale = locale
        resource = ResourceBundle.getBundle(baseName, locale)
    }

    constructor(languageTag: String) {
        this.locale = Locale.forLanguageTag(languageTag.replace("_", "-"))
        resource = ResourceBundle.getBundle("bundles/common/bundle", locale)
    }

    constructor(languageTag: String, source: ResourceBundle) {
        this.locale = Locale.forLanguageTag(languageTag.replace("_", "-"))
        resource = source
    }

    operator fun get(key: String): String {
        if (!resource.containsKey(key)) return key

        return if (prefix.isEmpty()) {
            MessageFormat(resource.getString(key), locale).format(arrayOf<Any>())
        } else {
            "$prefix " + MessageFormat(resource.getString(key), locale).format(arrayOf<Any>())
        }
    }

    operator fun get(key: String, vararg parameter: Any): String {
        if (!resource.containsKey(key)) return key

        return if (prefix.isEmpty()) {
            MessageFormat(resource.getString(key), locale).format(parameter)
        } else {
            "$prefix " + MessageFormat(resource.getString(key), locale).format(parameter)
        }
    }
}
