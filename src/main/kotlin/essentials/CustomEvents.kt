package essentials

class CustomEvents {
    class PlayerVoteKicked(val name : String, val voteReason : String, val online : String)
    class PlayerVoteBanned(val name : String, val voteReason : String, val online : String)
    class PlayerTempUnbanned(val name : String)
    class PlayerTempBanned(val name : String, val by : String, val date : String)
    class PlayerUnbanned(val by : String, val name : String, val date : String)
    class PlayerBanned(val name : String, val uuid : String, val date : String)
    class PlayerNameChanged(val oldName : String, val newName : String, val uuid : String)
}