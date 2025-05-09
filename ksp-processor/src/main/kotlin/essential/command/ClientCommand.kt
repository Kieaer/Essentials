package essential.command

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClientCommand(val name: String, val parameter: String = "", val description: String)