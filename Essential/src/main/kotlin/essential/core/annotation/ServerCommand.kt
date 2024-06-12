package essential.core.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class ServerCommand(val name: String, val parameter: String = "", val description: String)