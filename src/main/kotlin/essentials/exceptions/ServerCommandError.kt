package exceptions

class ServerCommandError : Throwable() {
    override val cause: Throwable?
        get() = super.cause
    override val message: String?
        get() = super.message
}