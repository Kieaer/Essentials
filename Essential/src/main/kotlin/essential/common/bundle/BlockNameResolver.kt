package essential.common.bundle

import java.util.Locale
import java.util.ResourceBundle

class BlockNameResolver(languageTag: String) {
    private val locale = Locale.forLanguageTag(languageTag.replace('_', '-'))
    private val control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)
    private val common = Bundle(
        locale.toLanguageTag(), ResourceBundle.getBundle("bundles/common/bundle", locale, control)
    )
    private val game = Bundle(
        locale.toLanguageTag(), ResourceBundle.getBundle("bundles/mindustry/bundle", locale, control)
    )

    operator fun get(blockName: String): String {
        if (blockName == "air") return common["event.log.air"]
        val key = "block.$blockName.name"
        val localized = game[key]
        return if (localized == key) blockName else localized
    }
}
