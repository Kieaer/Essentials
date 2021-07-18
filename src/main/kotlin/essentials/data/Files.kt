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
            json.add("siliconSmelter", 2, "Crafting")
            json.add("siliconCrucible", 6)
            json.add("kiln", 2)
            json.add("graphitePress", 2)
            json.add("plastaniumCompressor", 6)
            json.add("multiPress", 6)
            json.add("phaseWeaver", 15)
            json.add("surgeSmelter", 10)
            json.add("pyratiteMixer", 5)
            json.add("blastMixer", 7)
            json.add("cryofluidMixer", 6)
            json.add("melter", 2)
            json.add("separator", 3)
            json.add("disassembler",4)
            json.add("sporePress", 5)
            json.add("pulverizer", 2)
            json.add("incinerator", 1)
            json.add("coalCentrifuge", 1)

            // 샌드박스 아이템들
            json.add("powerSource", 0, "Sandbox")
            json.add("powerVoid", 0)
            json.add("itemSource", 0)
            json.add("itemVoid", 0)
            json.add("liquidSource", 0)
            json.add("liquidVoid", 0)
            json.add("payloadVoid", 0)
            json.add("payloadSource", 0)
            json.add("illuminator", 1)

            // 방어 건물
            json.add("copperWall", 1, "Defense")
            json.add("copperWallLarge", 4)
            json.add("titaniumWall", 2)
            json.add("titaniumWallLarge", 8)
            json.add("plastaniumWall", 3)
            json.add("plastaniumWallLarge", 12)
            json.add("thoriumWall", 4)
            json.add("thoriumWallLarge", 16)
            json.add("door", 1)
            json.add("doorLarge", 4)
            json.add("phaseWall", 5)
            json.add("phaseWallLarge", 20)
            json.add("surgeWall", 6)
            json.add("surgeWallLarge", 24)
            json.add("mender", 2)
            json.add("mendProjector", 4)
            json.add("overdriveProjector", 15)
            json.add("overdriveDome", 45)
            json.add("forceProjector", 10)
            json.add("shockMine", 2)
            json.add("scrapWall", 0)
            json.add("scrapWallLarge", 0)
            json.add("scrapWallHuge", 0)
            json.add("scrapWallGigantic", 0)
            json.add("thruster", 0)

            // 수송 건물
            json.add("conveyor", 0, "Transport")
            json.add("titaniumConveyor", 1)
            json.add("plastaniumConveyor", 2)
            json.add("armoredConveyor", 3)
            json.add("distributor", 1)
            json.add("junction", 1)
            json.add("itemBridge", 2)
            json.add("phaseConveyor", 5)
            json.add("sorter", 1)
            json.add("invertedSorter", 2)
            json.add("router", 0)
            json.add("overflowGate", 2)
            json.add("underflowGate", 2)
            json.add("massDriver", 20)
            json.add("duct", 0)
            json.add("ductRouter", 2)
            json.add("ductBridge", 2)

            // 액체 건물
            json.add("mechanicalPump", 1, "Drills")
            json.add("rotaryPump", 1)
            json.add("thermalPump", 3)
            json.add("conduit", 1)
            json.add("pulseConduit", 3)
            json.add("platedConduit", 4)
            json.add("liquidRouter", 1)
            json.add("liquidTank", 10)
            json.add("liquidJunction", 1)
            json.add("bridgeConduit", 2)
            json.add("phaseConduit", 4)

            // 전력
            json.add("combustionGenerator", 1, "Power")
            json.add("thermalGenerator", 9)
            json.add("steamGenerator", 5)
            json.add("differentialGenerator", 10)
            json.add("rtgGenerator", 8)
            json.add("solarPanel", 6)
            json.add("largeSolarPanel", 25)
            json.add("thoriumReactor", 30)
            json.add("impactReactor", 100)
            json.add("battery", 1)
            json.add("batteryLarge", 9)
            json.add("powerNode", 1)
            json.add("powerNodeLarge", 4)
            json.add("surgeTower", 15)
            json.add("diode", 1)

            // 생산 건물
            json.add("mechanicalDrill", 2, "Production")
            json.add("pneumaticDrill", 4)
            json.add("laserDrill", 10)
            json.add("blastDrill", 20)
            json.add("waterExtractor", 4)
            json.add("oilExtractor", 7)
            json.add("cultivator", 5)

            // 저장
            json.add("coreShard", 0, "Storage")
            json.add("coreFoundation", 0)
            json.add("coreNucleus", 0)
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
            json.add("commandCenter", 20, "Units")
            json.add("groundFactory", 10)
            json.add("airFactory", 10)
            json.add("navalFactory", 20)
            json.add("additiveReconstructor", 30)
            json.add("multiplicativeReconstructor", 50)
            json.add("exponentialReconstructor", 100)
            json.add("tetrativeReconstructor", 200)
            json.add("repairPoint", 7)
            json.add("resupplyPoint", 7)

            // 화물
            json.add("payloadConveyor", 3, "Payloads")
            json.add("payloadRouter", 3)
            json.add("payloadPropulsionTower", 3)

            // 로직
            json.add("message", 0, "Logic")
            json.add("switchBlock", 0)
            json.add("microProcessor", 0)
            json.add("logicProcessor", 0)
            json.add("hyperProcessor", 0)
            json.add("largeLogicDisplay", 0)
            json.add("logicDisplay", 0)
            json.add("memoryCell", 0)
            json.add("memoryBank", 0)

            // 캠페인
            json.add("launchPad", 0, "Campaign")
            json.add("interplanetaryAccelerator", 0)

            // 그 외 건물들
            json.add("blockForge", 2, "Misc experimental")
            json.add("blockLoader", 2)
            json.add("blockUnloader", 2)

            if(!pluginRoot.child("BlockReqExp.hjson").exists()) {
                pluginRoot.child("BlockReqExp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
            }
            if(!pluginRoot.child("Exp.hjson").exists()) {
                pluginRoot.child("Exp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
            }
        }

        if(!pluginRoot.child("permission.hjson").exists()) {
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

            pluginRoot.child("permission.hjson").writeString(json.toString(Stringify.HJSON))
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