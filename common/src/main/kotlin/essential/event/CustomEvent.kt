package essential.event

import essential.database.data.PlayerData
import java.nio.file.WatchEvent

object CustomEvents {
    class PlayerVoteKicked(val voteStarter: String, val name: String, val voteReason: String, val online: String)
    class PlayerVoteBanned(val voteStarter: String, val name: String, val voteReason: String, val online: String)
    class PlayerTempUnbanned(val name: String)
    class PlayerTempBanned(val name: String, val by: String, val date: String)
    class PlayerUnbanned(val name: String, val date: String)
    class PlayerBanned(val name: String, val uuid: String, val date: String, val reason: String)
    class PlayerNameChanged(val oldName: String, val newName: String, val uuid: String)
    class DiscordURLOpen(val playerData: PlayerData)
    class PlayerConnectKicked(val name: String, val reason: String)
    class PlayerReported(val reporter: String, val name: String, val reason: String)
    class PlayerDataLoad(val playerData: PlayerData)
    class PlayerDataLoadEnd(val playerData: PlayerData)
    class ConfigFileModified(val kind: WatchEvent.Kind<out Any>, val paths: String)
    class PlayerDiscordRequested(val uuid: String)
}