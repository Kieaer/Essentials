package essential.core

import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import com.charleskorn.kaml.Yaml
import mindustry.Vars
import mindustry.mod.Plugin
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.util.*


class Essential : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config.yaml"

        lateinit var conf: Config
        val bundle = Bundle()
    }

    init {
        Thread.currentThread().name = "Essential"
        bundle.prefix = "[Essential]"
    }

    val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

    override fun init() {
        Log.debug(bundle["event.plugin.starting"])
        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(Essential::class.java.getResourceAsStream("/config.yaml")!!, false)
        }

        conf = Yaml.default.decodeFromString(Config.serializer(), root.child(CONFIG_PATH).readString())
        bundle.locale = Locale(conf.plugin.lang)

        // DB 설정
        val database = DB()
        database.load()
        database.connect()
        database.create()

        // 데이터 설정
        PluginData.load()

        // 업데이트 확인
        if (conf.plugin.autoUpdate) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000).error { _ -> Log.warn(
                bundle["event.plugin.update.check.failed"]) }.block {
                if (it.status == Http.HttpStatus.OK) {
                    val json = JsonValue.readJSON(it.resultAsString).asObject()
                    Vars.mods.list().forEach { mod ->
                        if (mod.meta.name == "Essentials") {
                            PluginData.pluginVersion = mod.meta.version
                            return@forEach
                        }
                    }
                    val latest = DefaultArtifactVersion(json.getString("tag_name", PluginData.pluginVersion))
                    val current = DefaultArtifactVersion(PluginData.pluginVersion)

                    when {
                        latest > current -> Log.info(bundle["config.update.new", json["assets"].asArray()[0].asObject()["browser_download_url"].asString(), json["body"].asString()])
                        latest.compareTo(current) == 0 -> Log.info(bundle["config.update.current"])
                        latest < current -> Log.info(bundle["config.update.devel"])
                    }
                }
            }
        } else {
            Vars.mods.list().forEach { mod ->
                if (mod.meta.name == "Essentials") {
                    PluginData.pluginVersion = mod.meta.version
                    return@forEach
                }
            }
        }
        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        super.registerServerCommands(handler)
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        super.registerClientCommands(handler)
    }
}