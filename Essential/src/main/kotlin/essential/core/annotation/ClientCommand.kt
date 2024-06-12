package essential.core.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class ClientCommand(val name: String, val parameter: String = "", val description: String)