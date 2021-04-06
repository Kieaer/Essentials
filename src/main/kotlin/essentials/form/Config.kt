package essentials.form

abstract class Config {
    abstract fun createFile()
    abstract fun save()
    abstract fun load()
}