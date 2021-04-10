package essentials.data

import essentials.Main.Companion.pluginRoot
import essentials.form.Configs
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify

object Files : Configs() {
    override fun createFile() {
        if (!pluginRoot.exists()) {
            // 기록 파일들
            if (!pluginRoot.child("log").exists()) {
                val names = arrayOf(
                    "block",
                    "chat",
                    "deposit",
                    "error",
                    "griefer",
                    "non-block",
                    "player",
                    "tap",
                    "web",
                    "withdraw"
                )
                for (a in names) {
                    if (!pluginRoot.child("log/$a.log").exists()) {
                        pluginRoot.child("log/$a.log").file().createNewFile()
                    }
                }
            }
            // motd
            if (!pluginRoot.child("motd").exists()) {
                val names = arrayListOf("en_US", "ko_KR")
                val texts = arrayListOf(
                    "To edit this message, open [green]config/mods/Essentials/motd[] folder and edit [green]en_US.txt[]",
                    "이 메세지를 수정할려면 [green]config/mods/Essentials/motd[] 폴더에서 [green]ko_KR.txt[] 파일을 수정하세요."
                )
                for (a in 0..names.size) {
                    if (!pluginRoot.child("motd/$a.txt").exists()) {
                        pluginRoot.child("motd/$a.txt").writeString(texts[a])
                    }
                }
            }
            // 블록 설치시 요구레벨 설정 파일
            if (!pluginRoot.child("BlockReqExp.hjson").exists() || !pluginRoot.child("Exp.hjson").exists()) {
                val json = JsonObject()

                // 제작 건물
                json.add("siliconSmelter", 0, "Crafting")
                json.add("siliconCrucible", 0)
                json.add("kiln", 0)
                json.add("graphitePress", 0)
                json.add("plastaniumCompressor", 0)
                json.add("multiPress", 0)
                json.add("phaseWeaver", 0)
                json.add("surgeSmelter", 0)
                json.add("pyratiteMixer", 0)
                json.add("blastMixer", 0)
                json.add("cryofluidMixer", 0)
                json.add("melter", 0)
                json.add("separator", 0)
                json.add("disassembler", 0)
                json.add("sporePress", 0)
                json.add("pulverizer", 0)
                json.add("incinerator", 0)
                json.add("coalCentrifuge", 0)

                // 샌드박스 아이템들
                json.add("powerSource", 0, "Sandbox")
                json.add("powerVoid", 0)
                json.add("itemSource", 0)
                json.add("itemVoid", 0)
                json.add("liquidSource", 0)
                json.add("liquidVoid", 0)
                json.add("illuminator", 0)

                // 방어 건물
                json.add("copperWall", 0, "Defense")
                json.add("copperWallLarge", 0)
                json.add("titaniumWall", 0)
                json.add("titaniumWallLarge", 0)
                json.add("plastaniumWall", 0)
                json.add("plastaniumWallLarge", 0)
                json.add("thoriumWall", 0)
                json.add("thoriumWallLarge", 0)
                json.add("door", 0)
                json.add("doorLarge,phaseWall", 0)
                json.add("phaseWallLarge", 0)
                json.add("surgeWall", 0)
                json.add("surgeWallLarge", 0)
                json.add("mender", 0)
                json.add("mendProjector", 0)
                json.add("overdriveProjector", 0)
                json.add("overdriveDome", 0)
                json.add("forceProjector", 0)
                json.add("shockMine", 0)
                json.add("scrapWall", 0)
                json.add("scrapWallLarge", 0)
                json.add("scrapWallHuge", 0)
                json.add("scrapWallGigantic", 0)
                json.add("thruster", 0)

                // 수송 건물
                json.add("conveyor", 0, "Transport")
                json.add("titaniumConveyor", 0)
                json.add("plastaniumConveyor", 0)
                json.add("armoredConveyor", 0)
                json.add("distributor", 0)
                json.add("junction", 0)
                json.add("itemBridge", 0)
                json.add("phaseConveyor", 0)
                json.add("sorter", 0)
                json.add("invertedSorter", 0)
                json.add("router", 0)
                json.add("overflowGate", 0)
                json.add("underflowGate", 0)
                json.add("massDriver", 0)
                json.add("payloadConveyor", 0)
                json.add("payloadRouter", 0)

                // 액체 건물
                json.add("mechanicalPump", 0, "Liquid")
                json.add("rotaryPump", 0)
                json.add("thermalPump", 0)
                json.add("conduit", 0)
                json.add("pulseConduit", 0)
                json.add("platedConduit", 0)
                json.add("liquidRouter", 0)
                json.add("liquidTank", 0)
                json.add("liquidJunction", 0)
                json.add("bridgeConduit", 0)
                json.add("phaseConduit", 0)

                // 전력
                json.add("combustionGenerator", 0, "Power")
                json.add("thermalGenerator", 0)
                json.add("steamGenerator", 0)
                json.add("differentialGenerator", 0)
                json.add("rtgGenerator", 0)
                json.add("solarPanel", 0)
                json.add("largeSolarPanel", 0)
                json.add("thoriumReactor", 0)
                json.add("impactReactor", 0)
                json.add("battery", 0)
                json.add("batteryLarge", 0)
                json.add("powerNode", 0)
                json.add("powerNodeLarge", 0)
                json.add("surgeTower", 0)
                json.add("diode", 0)

                // 생산 건물
                json.add("mechanicalDrill", 0, "Production")
                json.add("pneumaticDrill", 0)
                json.add("laserDrill", 0)
                json.add("blastDrill", 0)
                json.add("waterExtractor", 0)
                json.add("oilExtractor", 0)
                json.add("cultivator", 0)

                // 저장
                json.add("coreShard", 0, "Storage")
                json.add("coreFoundation", 0)
                json.add("coreNucleus", 0)
                json.add("vault", 0)
                json.add("container", 0)
                json.add("unloader", 0)

                // 포탑
                json.add("duo", 0, "Turret")
                json.add("scatter", 0)
                json.add("scorch", 0)
                json.add("hail", 0)
                json.add("arc", 0)
                json.add("wave", 0)
                json.add("lancer", 0)
                json.add("swarmer", 0)
                json.add("salvo", 0)
                json.add("fuse", 0)
                json.add("ripple", 0)
                json.add("cyclone", 0)
                json.add("foreshadow", 0)
                json.add("spectre", 0)
                json.add("meltdown", 0)
                json.add("segment", 0)
                json.add("parallax", 0)
                json.add("tsunami", 0)

                // 유닛
                json.add("commandCenter", 0, "Units")
                json.add("groundFactory", 0)
                json.add("airFactory", 0)
                json.add("navalFactory", 0)
                json.add("additiveReconstructor", 0)
                json.add("multiplicativeReconstructor", 0)
                json.add("exponentialReconstructor", 0)
                json.add("tetrativeReconstructor", 0)
                json.add("repairPoint", 0)
                json.add("resupplyPoint", 0)

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
                json.add("launchPadLarge", 0)
                json.add("interplanetaryAccelerator", 0)

                // 그 외 건물들
                json.add("blockForge", 0, "Misc experimental")
                json.add("blockLoader", 0)
                json.add("blockUnloader", 0)

                if (!pluginRoot.child("BlockReqExp.hjson").exists()) {
                    pluginRoot.child("BlockReqExp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
                } else if (!pluginRoot.child("Exp.hjson").exists()) {
                    pluginRoot.child("Exp.hjson").writeString(json.toString(Stringify.HJSON_COMMENTS))
                }
            }

            if (!pluginRoot.child("permission.hjson").exists()) {
                val json = JsonObject()

                val owner = JsonObject()
                owner.add("admin", true)
                owner.add("prefix", "[sky][Owner] %1[orange] > [white]%2")
                owner.add("permissions", JsonArray().add("all"))

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
                admin.add("prefix", "[yellow][Admin] %1[orange] > [white]%2")
                admin.add("permissions", adminPerm)

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
                user.add("prefix", "%1[orange] > [white]%2")
                user.add("permissions", userPerm)

                val visitor = JsonObject()
                val visitorPerm = JsonArray()
                visitorPerm.add("register")
                visitorPerm.add("login")

                visitor.add("prefix", "%1[scarlet] > [white]%2")
                visitor.add("permissions", visitorPerm)

                json.add("owner", owner)
                json.add("admin", admin)
                json.add("user", user)
                json.add("visitor", visitor)

                pluginRoot.child("permission.hjson").writeString(json.toString(Stringify.HJSON))
            }

            if (!pluginRoot.child("permission_user.hjson").exists()) {
                pluginRoot.child("permission_user.hjson").writeString("{}")
            }
        }
    }

    override fun save() {
        TODO("Not yet implemented")
    }

    override fun load() {
        TODO("Not yet implemented")
    }
}