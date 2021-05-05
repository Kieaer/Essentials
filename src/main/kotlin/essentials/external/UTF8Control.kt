package essentials.external

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

// Source from https://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
class UTF8Control : ResourceBundle.Control() {
    override fun getFallbackLocale(aBaseName: String, aLocale: Locale): Locale {
        return Locale.getDefault()
    }

    override fun newBundle(baseName: String, locale: Locale, format: String, loader: ClassLoader, reload: Boolean): ResourceBundle { // The below is a copy of the default implementation.
        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")
        var bundle: ResourceBundle? = null
        val stream = loader.getResourceAsStream(resourceName)
        if(stream != null) {
            bundle = stream.use { s -> // Only this line is changed to make it to read properties files as UTF-8.
                PropertyResourceBundle(InputStreamReader(s, StandardCharsets.UTF_8))
            }
        }
        return bundle!!
    }
}