package essential.bridge

class Config {
    var address: String? = null
    var port: Int? = null
    var sharing: SharingConfig? = null

    internal class SharingConfig {
        var ban: Boolean = false
        var broadcast: Boolean = false
    }
}
