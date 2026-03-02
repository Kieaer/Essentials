package essential.common.config

import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.common.bundle
import essential.common.rootPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


object Config {
    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    /**
     * Load configuration from a YAML file.
     *
     * @param name YAML file name in the config folder
     * @param serializer Serializable configuration class
     * @param defaultConfig Default configuration when the file does not exist
     * @return Returns the configuration when loaded successfully, otherwise null
     */
    inline fun <reified T> load(
        name: String,
        serializer: KSerializer<T>,
        defaultConfig: T? = null
    ): T? {
        val name = "$name.yaml"
        val file = rootPath.child("config/$name").file()

        if (!file.exists()) {
            if (defaultConfig != null) {
                try {
                    rootPath.child("config").mkdirs()

                    save(name, serializer, defaultConfig)
                    Log.info(bundle["config.created", name])
                    return defaultConfig
                } catch (e: IOException) {
                    Log.err(bundle["config.create.failed", name], e)
                    return null
                }
            } else {
                Log.warn(bundle["config.not.found", name])
                return null
            }
        }

        return try {
            val content = Files.readString(Paths.get(rootPath.child("config/$name").absolutePath()))
            val config = yaml.decodeFromString(serializer, content)
            Log.info(bundle["config.loaded", name])
            config
        } catch (e: IOException) {
            Log.err(bundle["config.load.failed", name], e)
            null
        } catch (e: SerializationException) {
            Log.err(bundle["config.parse.failed", name], e)
            null
        }
    }

    /**
     * Save the configuration as YAML.
     *
     * @param name YAML file name in the config folder
     * @param serializer Serializable configuration class
     * @param config Configuration object to save
     * @return true if saved successfully, otherwise false
     */
    fun <T> save(name: String, serializer: KSerializer<T>, config: T): Boolean {
        val file = rootPath.child("config/${name}")

        return try {
            val content = yaml.encodeToString(serializer, config)
            file.writeString(content, false)
            Log.info(bundle["config.saved", name])
            true
        } catch (e: IOException) {
            Log.err(bundle["config.save.failed", name], e)
            false
        } catch (e: SerializationException) {
            Log.err(bundle["config.serialize.failed", name], e)
            false
        }
    }

    /**
     * Watch for changes to the configuration file.
     *
     * @param name Configuration file name in the config folder
     * @param serializer Serializable configuration class
     * @param onReload Code to run when the configuration file is modified
     */
    inline fun <reified T> watch(
        name: String,
        serializer: KSerializer<T>,
        crossinline onReload: (T) -> Unit
    ) {
        Log.info(bundle["config.watch", name])
    }

    /**
     * Migrate old configuration files to the new location.
     */
    fun migrate() {
        val marker = rootPath.child("config/.migrated")
        if (marker.exists()) return

        val oldDirPath = "D:\\민더\\config\\mods\\Essentials\\config"
        val oldDir = Paths.get(oldDirPath).toFile()
        val newDir = rootPath.child("config")

        if (oldDir.exists() && oldDir.isDirectory) {
            Log.info("[Essential] Old configuration found, starting migration...")
            val configFiles = listOf(
                "config.yaml",
                "config_bridge.yaml",
                "config_chat.yaml",
                "config_discord.yaml",
                "config_protect.yaml",
                "config_web.yaml"
            )

            if (!newDir.exists()) newDir.mkdirs()

            for (fileName in configFiles) {
                val oldFile = oldDir.resolve(fileName)
                val newFile = newDir.child(fileName).file()

                if (oldFile.exists()) {
                    try {
                        Files.copy(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        Log.info("[Essential] Migrated config file: $fileName")
                    } catch (e: Exception) {
                        Log.err("[Essential] Failed to migrate config file: $fileName", e)
                    }
                }
            }
            marker.writeString(System.currentTimeMillis().toString())
        }
    }
}
