package essential.chat

class Config {
    var chatFormat: String? = null
    var strict: StrictConfig? = null
    var blacklist: BlacklistConfig? = null

    internal class StrictConfig {
        var enabled: Boolean? = null
        var language: String? = null
    }

    internal class BlacklistConfig {
        var enabled: Boolean? = null
        var regex: Boolean? = null
    }
}
