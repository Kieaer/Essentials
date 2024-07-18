package essential.core.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ServerCommand(val name: String, val parameter: String = "", val description: String)