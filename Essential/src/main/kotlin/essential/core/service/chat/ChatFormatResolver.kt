package essential.core.service.chat

import essential.common.database.data.PlayerData

/**
 * Resolves chat format placeholders like %player.name, %player.level, %chat
 * using the provided PlayerData.
 *
 * Supported variables:
 *  - %player.name: display name
 *  - %player.uuid: player UUID
 *  - %player.level: exp level
 *  - %player.exp: current exp
 *  - %player.permission: permission group
 *  - %player.playtime: total played time in seconds
 *  - %player.blockPlace: blocks placed count
 *  - %player.blockBreak: blocks broken count
 *  - %player.attackClear: attack mode clear count
 *  - %player.waveClear: wave mode clear count
 *  - %player.pvpWin: PvP win count
 *  - %player.pvpLose: PvP lose count
 *  - %player.pvpEliminated: PvP eliminated count
 *  - %player.pvpMvp: PvP MVP count
 *  - %player.attendance: consecutive attendance days
 *  - %player.language: language tag
 *  - %player.world: last played world name
 *  - %player.worldMode: last played world mode
 *  - %chat: the chat message content
 */
object ChatFormatResolver {

    private val placeholders = mapOf(
        "%player.name" to { data: PlayerData -> data.player.name() },
        "%player.uuid" to { data: PlayerData -> data.uuid },
        "%player.level" to { data: PlayerData -> data.level.toString() },
        "%player.exp" to { data: PlayerData -> data.exp.toString() },
        "%player.permission" to { data: PlayerData -> data.permission },
        "%player.playtime" to { data: PlayerData -> data.totalPlayed.toString() },
        "%player.blockPlace" to { data: PlayerData -> data.blockPlaceCount.toString() },
        "%player.blockBreak" to { data: PlayerData -> data.blockBreakCount.toString() },
        "%player.attackClear" to { data: PlayerData -> data.attackClear.toString() },
        "%player.waveClear" to { data: PlayerData -> data.waveClear.toString() },
        "%player.pvpWin" to { data: PlayerData -> data.pvpWinCount.toString() },
        "%player.pvpLose" to { data: PlayerData -> data.pvpLoseCount.toString() },
        "%player.pvpEliminated" to { data: PlayerData -> data.pvpEliminatedCount.toString() },
        "%player.pvpMvp" to { data: PlayerData -> data.pvpMvpCount.toString() },
        "%player.attendance" to { data: PlayerData -> data.attendanceDays.toString() },
        "%player.language" to { data: PlayerData -> data.languageTag },
        "%player.world" to { data: PlayerData -> data.lastPlayedWorldName ?: "unknown" },
        "%player.worldMode" to { data: PlayerData -> data.lastPlayedWorldMode ?: "unknown" }
    )

    /**
     * Replaces all known placeholders in [format] with actual values from [data] and [message].
     * Unknown placeholders are left as-is.
     */
    fun resolve(format: String, data: PlayerData, message: String): String {
        var resolved = format
        for ((key, value) in placeholders) {
            resolved = resolved.replace(key, value(data))
        }
        resolved = resolved.replace("%chat", message)
        return resolved
    }
}
