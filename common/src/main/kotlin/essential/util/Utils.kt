package essential.util

import essential.database.data.PlayerData
import essential.players
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mindustry.Vars
import mindustry.gen.Groups
import mindustry.gen.Playerc
import mindustry.net.Administration
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Get current time. The Format is "yyyy-MM-dd HH:mm:ss.SSS" */
@OptIn(ExperimentalTime::class)
fun currentTime(): String {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).toHString()
}


/** Get player information by UUID from the plugin */
fun findPlayerData(uuid: String): PlayerData? {
    return players.find { data -> data.uuid == uuid }
}

/** Get player information by name from the plugin and return player */
fun findPlayers(name: String): Playerc? {
    if (name.toIntOrNull() != null) {
        players.forEach {
            if (it.entityId == name.toInt()) {
                return it.player
            }
        }
        return Groups.player.find { p -> p.id() == name.toInt() }
    } else {
        return Groups.player.find { p -> p.plainName().contains(name, true) }
    }
}

/** Get player information by name from built-in server info */
fun findPlayersByName(name: String): Administration.PlayerInfo? {
    return if (!Vars.netServer.admins.findByName(name).isEmpty) {
        Vars.netServer.admins.findByName(name).first()
    } else {
        null
    }
}