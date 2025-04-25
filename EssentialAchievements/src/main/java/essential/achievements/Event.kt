package essential.achievements

import arc.Events

class Event {
    var tick: kotlin.Int = 0

    @essential.core.annotation.Event
    fun blockBuildEnd() {
        Events.on(EventType.BlockBuildEndEvent::class.java, { e ->
            if (e.unit.isPlayer()) {
                val data: PlayerData? = findPlayerByUuid(e.unit.getPlayer().uuid())
                if (data != null) {
                    if (Achievement.Builder.success(data)) {
                        Achievement.Builder.set(data)
                    }
                    if (Achievement.Deconstructor.success(data)) {
                        Achievement.Deconstructor.set(data)
                    }
                }
            }
        })
    }

    @essential.core.annotation.Event
    fun gameover() {
        Events.on(EventType.GameOverEvent::class.java, { e ->
            database.getPlayers().forEach({ data ->
                if (Achievement.Eliminator.success(data)) {
                    Achievement.Eliminator.set(data)
                }
                if (Achievement.Lord.success(data)) {
                    Achievement.Lord.set(data)
                }

                if (Achievement.Aggressor.success(data)) {
                    Achievement.Aggressor.set(data)
                }
                if (e.winner === data.getPlayer().team()) {
                    if (Achievement.Asteroids.success(data)) {
                        Achievement.Asteroids.set(data)
                    }
                }
            })
        })
    }

    @essential.core.annotation.Event
    fun wave() {
        Events.on(EventType.WaveEvent::class.java, { e ->
            database.getPlayers().forEach({ data ->
                val value = data.getStatus().getOrDefault("record.wave", "0").toInt() + 1
                data.getStatus().put("record.wave", value.toString())
                if (Achievement.Defender.success(data)) {
                    Achievement.Defender.set(data)
                }
            })
        })
    }

    @essential.core.annotation.Event
    fun achievementClear() {
        Events.on(AchievementClear::class.java, { e ->
            val locale: java.util.Locale?
            if (e.playerData.getStatus().containsKey("language")) {
                locale = java.util.Locale(e.playerData.getStatus().get("language"))
            } else {
                locale = java.util.Locale(e.playerData.getPlayer().locale())
            }

            val bundle: Bundle = Bundle(java.util.ResourceBundle.getBundle("bundle", locale))

            e.playerData.send(bundle, "event.achievement.success", e.achievement.toString().toLowerCase())
            database.getPlayers().forEach({ data ->
                val b: Bundle = Bundle(
                    java.util.ResourceBundle.getBundle(
                        "bundle",
                        java.util.Locale(data.getPlayer().locale()),
                        essential.achievements.Main::class.java.getClassLoader()
                    )
                )
                data.send(
                    b,
                    "event.achievement.success.other",
                    e.playerData.getName(),
                    b.get("achievement." + e.achievement.toString().toLowerCase())
                )
            })
        })
    }

    @essential.core.annotation.Event
    fun playerChat() {
        Events.on(EventType.PlayerChatEvent::class.java, { e ->
            if (!e.message.startsWith("/")) {
                val data: PlayerData? = findPlayerByUuid(e.player.uuid())
                if (data != null) {
                    val value = data.getStatus().getOrDefault("record.time.chat", "0").toInt() + 1
                    data.getStatus().put("record.time.chat", value.toString())
                    if (Achievement.Chatter.success(data)) {
                        Achievement.Chatter.set(data)
                    }
                }
            }
        })
    }

    @essential.core.annotation.Event
    fun updateSecond() {
        Timer.schedule({
            for (data in database.getPlayers()) {
                if (state.rules.planet === Planets.serpulo) {
                    val value = data.getStatus().getOrDefault("record.time.serpulo", "0").toInt() + 1
                    data.getStatus().put("record.time.serpulo", value.toString())
                    if (Achievement.Serpulo.success(data)) {
                        Achievement.Serpulo.set(data)
                    }
                } else if (state.rules.planet === Planets.erekir) {
                    val value = data.getStatus().getOrDefault("record.time.erekir", "0").toInt() + 1
                    data.getStatus().put("record.time.erekir", value.toString())
                    if (Achievement.Erekir.success(data)) {
                        Achievement.Erekir.set(data)
                    }
                } else if (state.rules.infiniteResources) {
                    val value = data.getStatus().getOrDefault("record.time.sandbox", "0").toInt() + 1
                    data.getStatus().put("record.time.sandbox", value.toString())
                    if (Achievement.Creator.success(data)) {
                        Achievement.Creator.set(data)
                    }
                }
            }
            var isOwnerMeet = false

            for (data in database.getPlayers()) {
                if (data.getPermission().equals("owner")) {
                    isOwnerMeet = true
                    break
                }
            }
            if (isOwnerMeet) {
                for (data in database.getPlayers()) {
                    val value = data.getStatus().getOrDefault("record.time.meetowner", "0").toInt() + 1
                    data.getStatus().put("record.time.meetowner", value.toString())
                    if (Achievement.MeetOwner.success(data)) {
                        Achievement.MeetOwner.set(data)
                    }
                }
            }
        }, 0, 1)
    }

    fun findPlayerByUuid(uuid: kotlin.String?): PlayerData {
        return database.getPlayers().stream().filter({ e -> e.getUuid().equals(uuid) }).findFirst().orElse(null)
    }
}
