package essential.common.config

import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import essential.common.bundle
import essential.common.rootPath
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths


object Config {
    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    init {
        renameConfigsDirectory()
    }

    fun renameConfigsDirectory() {
        val oldDir = rootPath.child("configs")
        val newDir = rootPath.child("config")
        if (oldDir.exists() && !newDir.exists()) {
            try {
                oldDir.file().renameTo(newDir.file())
            } catch (e: Exception) {
                Log.err(e)
            }
        }
    }

    fun hasMissingKeys(userNode: YamlNode, canonicalNode: YamlNode): Boolean {
        if (userNode is YamlMap && canonicalNode is YamlMap) {
            val userKeys = userNode.entries.keys.map { it.content }.toSet()
            for ((keyNode, canonicalValue) in canonicalNode.entries) {
                val key = keyNode.content
                if (key !in userKeys) {
                    return true
                }
                val userValue = userNode.entries.entries.find { it.key.content == key }?.value
                if (userValue == null) {
                    return true
                }
                if (hasMissingKeys(userValue, canonicalValue)) {
                    return true
                }
            }
        } else if (canonicalNode is YamlMap) {
            return true
        }
        return false
    }

    /**
     * Check whether the user config file is missing any comment line that the canonical
     * (freshly serialized) content carries. Used to upgrade older comment-less config files
     * to the documented format on every startup.
     */
    fun hasMissingComments(userContent: String, canonicalContent: String): Boolean {
        val userComments = userContent.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("#") }
            .toSet()
        return canonicalContent.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("#") }
            .any { it !in userComments }
    }

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
            try {
                val userNode = yaml.parseToYamlNode(content)
                val canonicalContent = yaml.encodeToString(serializer, config)
                val canonicalNode = yaml.parseToYamlNode(canonicalContent)
                // Re-save when keys are missing (migration) or when the canonical comments
                // are absent from the user file (upgrade older comment-less configs).
                if (hasMissingKeys(userNode, canonicalNode) || hasMissingComments(content, canonicalContent)) {
                    save(name, serializer, config)
                }
            } catch (e: Exception) {
                Log.err("Error migrating config $name: ${e.message}")
            }
            config
        } catch (e: IOException) {
            null
        } catch (e: SerializationException) {
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
}