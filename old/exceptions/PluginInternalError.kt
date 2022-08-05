package exceptions

class PluginInternalError : Throwable() {
    override val cause: Throwable?
        get() = super.cause
    override val message: String?
        get() = super.message
}