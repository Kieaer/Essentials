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

object Config {
    val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    /**
     * yaml 파일으로부터 설정을 불러 옵니다.
     *
     * @param name config 폴더에서의 yaml 파일 이름
     * @param serializer 직렬화가 가능한 설정 class
     * @param defaultConfig 파일이 없을 경우 기본 설정
     * @return 설정 파일이 불러왔을 경우 해당 설정을 반환하고, 그렇지 않을 경우 null 을 반환
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
     * 설정을 yaml 으로 저장합니다.
     *
     * @param name config 폴더에서의 yaml 파일 이름
     * @param serializer 직렬화가 가능한 설정 class
     * @param config 저장할 설정 class
     * @return 저장에 성공 할 경우 true, 그렇지 않을 경우 false
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
     * 설정 파일이 변경 되었는지 확인
     *
     * @param name config 폴더에서의 설정 파일 이름
     * @param serializer 직렬화가 가능한 설정 class
     * @param onReload 설정 파일이 수정 되었을 경우 실행할 코드
     */
    inline fun <reified T> watch(
        name: String,
        serializer: KSerializer<T>,
        crossinline onReload: (T) -> Unit
    ) {
        Log.info(bundle["config.watch", name])
    }
}