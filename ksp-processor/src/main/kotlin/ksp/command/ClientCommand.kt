package ksp.command

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ClientCommand(val name: String, val parameter: String = "", val description: String)