package essentials

class CustomEvents {
    class PlayerVoteKicked(val voteStarter : String, val name : String, val voteReason : String, val online : String)
    class PlayerVoteBanned(val voteStarter : String, val name : String, val voteReason : String, val online : String)
    class PlayerTempUnbanned(val name : String)
    class PlayerTempBanned(val name : String, val by : String, val date : String)
    class PlayerUnbanned(val name : String, val date : String)
    class PlayerBanned(val name : String, val uuid : String, val date : String, val reason : String)
    class PlayerNameChanged(val oldName : String, val newName : String, val uuid : String)
    class DiscordURLOpen(val data : DB.PlayerData)
    class PlayerConnectKicked(val name : String, val reason : String)
    class PlayerReported(val reporter : String, val name : String, val reason : String)
}