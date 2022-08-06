package essentials.data

import essentials.Main
import essentials.Main.Companion.pluginRoot
import essentials.PluginData
import essentials.form.Configs
import mindustry.content.Blocks
import mindustry.ctype.ContentList
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.IOException

object Files : Configs() {
    @Throws(IOException::class)
    override fun createFile() {
        // 기록 파일들
        if(!pluginRoot.child("log").exists()) {
            val names = arrayOf("block", "chat", "deposit", "error", "griefer", "non-block", "player", "tap", "web", "withdraw")
            for(a in names) {
                if(!pluginRoot.child("log/$a.log").exists()) {
                    pluginRoot.child("log").mkdirs()
                    pluginRoot.child("log/$a.log").file().createNewFile()
                }
            }
        }
        // motd
        if(!pluginRoot.child("motd").exists()) {
            pluginRoot.child("motd").mkdirs()
            val names = arrayListOf("en_US", "ko_KR")
            val texts = arrayListOf("To edit this message, open [green]config/mods/Essentials/motd[] folder and edit [green]en_US.txt[]", "이 메세지를 수정할려면 [green]config/mods/Essentials/motd[] 폴더에서 [green]ko_KR.txt[] 파일을 수정하세요.")
            for(a in 0 until names.size) {
                if(!pluginRoot.child("motd/${names[a]}.txt").exists()) {
                    pluginRoot.child("motd/${names[a]}.txt").writeString(texts[a])
                }
            }
        }
        // 블록 설치시 요구레벨 설정 파일
        if(!pluginRoot.child("BlockReqExp.hjson").exists() || !pluginRoot.child("Exp.hjson").exists()) {
            val json = JsonObject()

            // 제작 건물
            json.add("silicon-smelter", 2, "Crafting")
            json.add("silicon-crucible", 6)
            json.add("kiln", 2)
            json.add("graphite-press", 2)
            json.add("plastanium-compressor", 6)
            json.add("multi-press", 6)
            json.add("phase-weaver", 15)
            json.add("surge-smelter", 10)
            json.add("pyratite-mixer", 5)
            json.add("blast-mixer", 7)
            json.add("cryofluid-mixer", 6)
            json.add("melter", 2)
            json.add("separator", 3)
            json.add("disassembler",4)
            json.add("spore-press", 5)
            json.add("pulverizer", 2)
            json.add("incinerator", 1)
            json.add("coal-centrifuge", 1)

            // 샌드박스 아이템들
            json.add("power-source", 0, "Sandbox")
            json.add("power-void", 0)
            json.add("item-source", 0)
            json.add("item-void", 0)
            json.add("liquid-source", 0)
            json.add("liquid-void", 0)
            json.add("payload-void", 0)
            json.add("payload-source", 0)
            json.add("illuminator", 1)

            // 방어 건물
            json.add("copper-wall", 1, "Defense")
            json.add("copper-wall-large", 4)
            json.add("titanium-wall", 2)
            json.add("titanium-wall-large", 8)
            json.add("plastanium-wall", 3)
            json.add("plastanium-wall-large", 12)
            json.add("thorium-wall", 4)
            json.add("thorium-wall-large", 16)
            json.add("door", 1)
            json.add("door-large", 4)
            json.add("phase-wall", 5)
            json.add("phase-wall-large", 20)
            json.add("surge-wall", 6)
            json.add("surge-wall-large", 24)
            json.add("mender", 2)
            json.add("mend-projector", 4)
            json.add("overdrive-projector", 15)
            json.add("overdrive-dome", 45)
            json.add("force-projector", 10)
            json.add("shock-mine", 2)
            json.add("scrap-wall", 0)
            json.add("scrap-wall-large", 0)
            json.add("scrap-wall-huge", 0)
            json.add("scrap-wall-gigantic", 0)
            json.add("thruster", 0)

            // 수송 건물
            json.add("conveyor", 0, "Transport")
            json.add("titanium-conveyor", 1)
            json.add("plastanium-conveyor", 2)
            json.add("armored-conveyor", 3)
            json.add("distributor", 1)
            json.add("junction", 1)
            json.add("item-bridge", 2)
            json.add("phase-conveyor", 5)
            json.add("sorter", 1)
            json.add("inverted-sorter", 2)
            json.add("router", 0)
            json.add("overflow-gate", 2)
            json.add("underflow-gate", 2)
            json.add("mass-driver", 20)
            json.add("duct", 0)
            json.add("duct-router", 2)
            json.add("duct-bridge", 2)

            // 액체 건물
            json.add("mechanical-pump", 1, "Drills")
            json.add("rotary-pump", 1)
            json.add("thermal-pump", 3)
            json.add("conduit", 1)
            json.add("pulse-conduit", 3)
            json.add("plated-conduit", 4)
            json.add("liquid-router", 1)
            json.add("liquid-tank", 10)
            json.add("liquid-junction", 1)
            json.add("bridge-conduit", 2)
            json.add("phase-conduit", 4)

            // 전력
            json.add("combustion-generator", 1, "Power")
            json.add("thermal-generator", 9)
            json.add("steam-generator", 5)
            json.add("differential-generator", 10)
            json.add("rtg-generator", 8)
            json.add("solar-panel", 6)
            json.add("large-solar-panel", 25)
            json.add("thorium-reactor", 30)
            json.add("impact-reactor", 100)
            json.add("battery", 1)
            json.add("battery-large", 9)
            json.add("power-node", 1)
            json.add("power-node-large", 4)
            json.add("surge-tower", 15)
            json.add("diode", 1)

            // 생산 건물
            json.add("mechanical-drill", 2, "Production")
            json.add("pneumatic-drill", 4)
            json.add("laser-drill", 10)
            json.add("blast-drill", 20)
            json.add("water-extractor", 4)
            json.add("oil-extractor", 7)
            json.add("cultivator", 5)

            // 저장
            json.add("core-shard", 0, "Storage")
            json.add("core-foundation", 0)
            json.add("core-nucleus", 0)
            json.add("vault", 15)
            json.add("container", 6)
            json.add("unloader", 1)

            // 포탑
            json.add("duo", 1, "Turret")
            json.add("scatter", 2)
            json.add("scorch", 3)
            json.add("hail", 4)
            json.add("arc", 5)
            json.add("wave", 4)
            json.add("lancer", 10)
            json.add("swarmer", 15)
            json.add("salvo", 12)
            json.add("fuse", 13)
            json.add("ripple", 20)
            json.add("cyclone", 25)
            json.add("foreshadow", 30)
            json.add("spectre", 40)
            json.add("meltdown", 50)
            json.add("segment", 70)
            json.add("parallax", 100)
            json.add("tsunami", 150)

            // 유닛
            json.add("command-center", 20, "Units")
            json.add("ground-factory", 10)
            json.add("air-factory", 10)
            json.add("naval-factory", 20)
            json.add("additive-reconstructor", 30)
            json.add("multiplicative-reconstructor", 50)
            json.add("exponential-reconstructor", 100)
            json.add("tetrative-reconstructor", 200)
            json.add("repair-point", 7)
            json.add("resupply-point", 7)

            // 화물
            json.add("payload-conveyor", 3, "Payloads")
            json.add("payload-router", 3)
            json.add("payload-propulsion-tower", 200)

            // 로직
            json.add("message", 0, "Logic")
            json.add("switch-block", 0)
            json.add("micro-processor", 0)
            json.add("logic-processor", 0)
            json.add("hyper-processor", 0)
            json.add("large-logic-display", 0)
            json.add("logic-display", 0)
            json.add("memory-cell", 0)
            json.add("memory-bank", 0)

            // 캠페인
            json.add("launch-pad", 0, "Campaign")
            json.add("interplanetary-accelerator", 0)

            // 그 외 건물들
            json.add("block-forge", 2, "Misc experimental")
            json.add("block-loader", 2)
            json.add("block-unloader", 2)

            if(!pluginRoot.child("BlockReqExp.hjson").exists()) {
                pluginRoot.child("BlockReqExp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
            }
            if(!pluginRoot.child("Exp.hjson").exists()) {
                pluginRoot.child("Exp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
            }
        }

        if(!pluginRoot.child("permission.txt").exists()) {
            val json = JsonObject()

            val owner = JsonObject()
            owner.add("admin", true)
            owner.add("chatFormat", "[sky][Owner] %1[orange] > [white]%2")
            owner.add("permission", JsonArray().add("all"))

            val admin = JsonObject()
            val adminPerm = JsonArray()
            adminPerm.add("color")
            adminPerm.add("spawn")
            adminPerm.add("weather")
            adminPerm.add("kill")
            adminPerm.add("team")
            adminPerm.add("mute")

            admin.add("inheritance", "user")
            admin.add("admin", true)
            admin.add("chatFormat", "[yellow][Admin] %1[orange] > [white]%2")
            admin.add("permission", adminPerm)

            val user = JsonObject()
            val userPerm = JsonArray()
            userPerm.add("ch")
            userPerm.add("info")
            userPerm.add("maps")
            userPerm.add("me")
            userPerm.add("motd")
            userPerm.add("players")
            userPerm.add("status")
            userPerm.add("time")
            userPerm.add("tp")
            userPerm.add("vote")

            user.add("inheritance", "visitor")
            user.add("chatFormat", "%1[orange] > [white]%2")
            user.add("permission", userPerm)

            val visitor = JsonObject()
            val visitorPerm = JsonArray()
            visitorPerm.add("register")
            visitorPerm.add("login")
            visitorPerm.add("help")
            visitorPerm.add("t")

            visitor.add("chatFormat", "%1[scarlet] > [white]%2")
            visitor.add("default", true)
            visitor.add("permission", visitorPerm)

            json.add("owner", owner)
            json.add("admin", admin)
            json.add("user", user)
            json.add("visitor", visitor)

            pluginRoot.child("permission.txt").writeString(json.toString(Stringify.HJSON))
        }

        if(!pluginRoot.child("permission_user.hjson").exists()) {
            pluginRoot.child("permission_user.hjson").writeString("{}")
        }

        load()
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    override fun load() {
        PluginData.expData = JsonValue.readHjson(Main.pluginRoot.child("Exp.hjson").reader()).asObject()
    }
}