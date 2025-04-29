package essential.config

import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.bundle
import essential.rootPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Utility class for loading and saving configuration files in YAML format.
 * Uses the charleskorn/kaml library for YAML serialization/deserialization.
 */
object Config {
    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    /**
     * Loads a configuration from a YAML file.
     *
     * @param path The path to the YAML file
     * @param serializer The serializer for the configuration class
     * @param createIfNotExists Whether to create a default configuration file if it doesn't exist
     * @param defaultConfig The default configuration to use if the file doesn't exist
     * @return The loaded configuration, or null if loading failed
     */
    inline fun <reified T> load(
        name: String,
        serializer: KSerializer<T>,
        createIfNotExists: Boolean = false,
        defaultConfig: T? = null
    ): T? {
        val file = rootPath.child("/config/$name.yaml").file()

        if (!file.exists()) {
            if (createIfNotExists && defaultConfig != null) {
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
            val content = Files.readString(Paths.get(name))
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
     * Saves a configuration to a YAML file.
     *
     * @param path The path to the YAML file
     * @param serializer The serializer for the configuration class
     * @param config The configuration to save
     * @return True if saving was successful, false otherwise
     */
    fun <T> save(path: String, serializer: KSerializer<T>, config: T): Boolean {
        val file = File(path)

        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()

        return try {
            val content = yaml.encodeToString(serializer, config)
            Files.writeString(Paths.get(path), content)
            Log.info(bundle["config.saved", path])
            true
        } catch (e: IOException) {
            Log.err(bundle["config.save.failed", path], e)
            false
        } catch (e: SerializationException) {
            Log.err(bundle["config.serialize.failed", path], e)
            false
        }
    }

    /**
     * Watches a configuration file for changes and reloads it when it changes.
     * This is a placeholder for future implementation.
     *
     * @param path The path to the YAML file
     * @param serializer The serializer for the configuration class
     * @param onReload Callback function to be called when the configuration is reloaded
     */
    inline fun <reified T> watch(
        path: String,
        serializer: KSerializer<T>,
        crossinline onReload: (T) -> Unit
    ) {
        // This is a placeholder for future implementation
        // The actual implementation would use FileWatchService to watch for file changes
        // and reload the configuration when it changes
        Log.info(bundle["config.watch", path])
    }
}