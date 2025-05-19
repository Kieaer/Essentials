package essential.command

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ServerCommand(val name: String, val parameter: String = "", val description: String)