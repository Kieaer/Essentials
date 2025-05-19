package essential.achievements

import arc.Events
import essential.bundle.Bundle
import essential.core.Main.Companion.scope
import essential.core.annotation.Event
import essential.database.data.PlayerData
import essential.players
import essential.util.startInfiniteScheduler
import kotlinx.coroutines.launch
import mindustry.Vars.state
import mindustry.content.Planets
import mindustry.game.EventType
import java.util.Locale

class Events {
    var tick: Int = 0

    @Event
    fun blockBuildEnd() {
        Events.on(EventType.BlockBuildEndEvent::class.java) { e ->
            if (e.unit.isPlayer) {
                val data: PlayerData? = findPlayerByUuid(e.unit.player.uuid())
                if (data != null) {
                    if (Achievement.Builder.success(data)) {
                        Achievement.Builder.set(data)
                    }
                    if (Achievement.Deconstructor.success(data)) {
                        Achievement.Deconstructor.set(data)
                    }
                }
            }
        }
    }

    @Event
    fun gameover() {
        Events.on(EventType.GameOverEvent::class.java, { e ->
            players.forEach { data ->
                if (Achievement.Eliminator.success(data)) {
                    Achievement.Eliminator.set(data)
                }
                if (Achievement.Lord.success(data)) {
                    Achievement.Lord.set(data)
                }

                if (Achievement.Aggressor.success(data)) {
                    Achievement.Aggressor.set(data)
                }
                if (e.winner === data.player.team()) {
                    if (Achievement.Asteroids.success(data)) {
                        Achievement.Asteroids.set(data)
                    }
                }
            }
        })
    }

    @essential.core.annotation.Event
    fun wave() {
        Events.on(EventType.WaveEvent::class.java, { e ->
            players.forEach { data ->
                val value = data.status.getOrDefault("record.wave", "0").toInt() + 1
                data.status.put("record.wave", value.toString())
                if (Achievement.Defender.success(data)) {
                    Achievement.Defender.set(data)
                }
            }
        })
    }

    @Event
    fun achievementClear() {
        Events.on(CustomEvents.AchievementClear::class.java, { e ->
            val bundle = Bundle(java.util.ResourceBundle.getBundle("bundle", Locale.of(e.playerData.player.locale())))

            e.playerData.send(bundle, "event.achievement.success", e.achievement.toString().lowercase())
            players.forEach { data ->
                val b = Bundle(
                    java.util.ResourceBundle.getBundle(
                        "bundle",
                        Locale.of(data.player.locale()),
                        Main::class.java.getClassLoader()
                    )
                )

                data.send(
                    b,
                    "event.achievement.success.other",
                    e.playerData.name,
                    b.get("achievement." + e.achievement.toString().lowercase())
                )
            }
        })
    }

    @Event
    fun playerChat() {
        Events.on(EventType.PlayerChatEvent::class.java, { e ->
            if (!e.message.startsWith("/")) {
                val data: PlayerData? = findPlayerByUuid(e.player.uuid())
                if (data != null) {
                    val value = data.status.getOrDefault("record.time.chat", "0").toInt() + 1
                    data.status.put("record.time.chat", value.toString())
                    if (Achievement.Chatter.success(data)) {
                        Achievement.Chatter.set(data)
                    }
                }
            }
        })
    }

    @Event
    fun unitChange() {
        Events.on(EventType.UnitChangeEvent::class.java, { e ->
            if (e.player != null && e.unit != null && state.rules.planet === Planets.serpulo) {
                val data: PlayerData? = findPlayerByUuid(e.player.uuid())
                if (data != null && e.unit.type.name.equals("quad", true)) {
                    data.status.put("record.unit.serpulo.quad", "1")
                    if (Achievement.SerpuloQuad.success(data)) {
                        Achievement.SerpuloQuad.set(data)
                    }
                }
            }
        })
    }

    @Event
    fun updateSecond() {
        scope.startInfiniteScheduler {
            for (data in players) {
                if (state.rules.planet === Planets.serpulo) {
                    val value = data.status.getOrDefault("record.time.serpulo", "0").toInt() + 1
                    data.status.put("record.time.serpulo", value.toString())
                    if (Achievement.Serpulo.success(data)) {
                        Achievement.Serpulo.set(data)
                    }
                } else if (state.rules.planet === Planets.erekir) {
                    val value = data.status.getOrDefault("record.time.erekir", "0").toInt() + 1
                    data.status.put("record.time.erekir", value.toString())
                    if (Achievement.Erekir.success(data)) {
                        Achievement.Erekir.set(data)
                    }
                } else if (state.rules.infiniteResources) {
                    val value = data.status.getOrDefault("record.time.sandbox", "0").toInt() + 1
                    data.status.put("record.time.sandbox", value.toString())
                    if (Achievement.Creator.success(data)) {
                        Achievement.Creator.set(data)
                    }
                }
            }
            var isOwnerMeet = false

            for (data in players) {
                if (data.permission.equals("owner")) {
                    isOwnerMeet = true
                    break
                }
            }

            if (isOwnerMeet) {
                for (data in players) {
                    val value = data.status.getOrDefault("record.time.meetowner", "0").toInt() + 1
                    data.status.put("record.time.meetowner", value.toString())
                    if (Achievement.MeetOwner.success(data)) {
                        Achievement.MeetOwner.set(data)
                    }
                }
            }
        }
    }

    fun findPlayerByUuid(uuid: kotlin.String?): PlayerData {
        return players.stream().filter({ e -> e.uuid == uuid }).findFirst().orElse(null)
    }
}
