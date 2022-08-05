package exceptions

class ClientCommandError : Throwable() {
    override val cause: Throwable?
        get() = super.cause
    override val message: String?
        get() = super.message
}