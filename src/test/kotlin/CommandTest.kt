import PluginTest.Companion.clientCommand
import PluginTest.Companion.path
import PluginTest.Companion.player
import arc.Core
import arc.Events
import essentials.DB
import essentials.Main.Companion.database
import essentials.Permission
import junit.framework.TestCase.*
import mindustry.Vars
import mindustry.game.EventType.PlayerJoin
import mindustry.game.EventType.PlayerLeave
import mindustry.game.Gamemode
import mindustry.gen.Player
import mindustry.gen.Playerc
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.junit.AfterClass
import org.junit.Test
import org.mindrot.jbcrypt.BCrypt
import java.lang.Thread.sleep

class CommandTest {
    companion object {
        @AfterClass
        @JvmStatic
        fun shutdown() {
            path.child("mods/Essentials").deleteDirectory()
            path.child("maps").deleteDirectory()
        }
    }

    var playerData : DB.PlayerData
    val test = PluginTest()

    init {
        test.loadGame()
        test.loadPlugin()

        val p = newPlayer()
        Vars.player = p.first.self()
        player = p.first.self()
        playerData = p.second
    }

    fun newPlayer() : Pair<Player, DB.PlayerData> {
        val player = test.createPlayer()
        Events.fire(PlayerJoin(player))

        // Wait for database add time
        sleep(500)
        return Pair(player, database.players.find { data -> data.uuid == player.uuid() })
    }

    fun newPlayerNotRegistered() : Playerc {
        return test.createPlayer()
    }

    fun leavePlayer(player : Playerc) {
        Events.fire(PlayerLeave(player.self()))

        // Wait for database save time
        sleep(500)
    }

    fun setPermission(group : String, admin : Boolean) {
        val json = JsonArray()
        val obj = JsonObject()
        obj.add("name", player.name())
        obj.add("uuid", player.uuid())
        obj.add("group", group)
        obj.add("admin", admin)
        json.add(obj)

        Core.settings.dataDirectory.child("mods/Essentials/permission_user.txt").writeString(json.toString())
        Permission.load()
    }

    @Test
    fun clientCommand_changemap() {
        // Require admin or adobe permission
        setPermission("owner", true)

        // If map not found
        clientCommand.handleMessage("/changemap nothing survival", player)
        assertEquals("[scarlet]nothing 맵을 찾을 수 없습니다!", playerData.lastSentMessage)

        // Number method
        clientCommand.handleMessage("/changemap 0 survival", player)
        assertEquals("Ancient Caldera", Vars.state.map.name())

        // Name method
        clientCommand.handleMessage("/changemap fork survival", player)
        assertEquals("Fork", Vars.state.map.name())

        // If player enter wrong gamemode
        clientCommand.handleMessage("/changemap fork creative", player)
        assertEquals("[scarlet]creative 게임 모드는 없는 모드 입니다!", playerData.lastSentMessage)

        // If player enter only map name
        clientCommand.handleMessage("/changemap glacier", player)
        assertEquals("Glacier", Vars.state.map.name())
        assertEquals(Gamemode.survival, Vars.state.rules.mode())
    }

    @Test
    fun clientCommand_changename() {
        // Require admin or adobe permission
        setPermission("owner", true)

        // Change self name
        clientCommand.handleMessage("/changename Kieaer", player)
        assertEquals("Kieaer", player.name())

        // Change other player name
        val registeredUser = newPlayer()
        clientCommand.handleMessage("/changename dummy ${registeredUser.first.name()}", player)
        assertEquals("dummy", registeredUser.first.name())
        leavePlayer(registeredUser.first)

        // If target player not found
        clientCommand.handleMessage("/changename eat yammi", player)
        assertEquals("[scarlet]대상 플레이어를 찾을 수 없습니다!", playerData.lastSentMessage)

        // If target player exists but registered
        val notRegisteredUser = newPlayerNotRegistered()
        val oldName = notRegisteredUser.name()
        clientCommand.handleMessage("/changename not ${registeredUser.first.name()}", player)
        assertEquals(oldName, notRegisteredUser.name())
        assertEquals("[scarlet]해당 플레이어가 계정 등록을 하지 않았습니다.", playerData.lastSentMessage)
        leavePlayer(notRegisteredUser)
    }

    @Test
    fun clientCommand_changepw() {
        // Require user or adobe permission
        setPermission("user", true)

        // Change password
        clientCommand.handleMessage("/changepw pass pass", player)
        assertTrue(BCrypt.checkpw("pass", playerData.accountPW))
        assertEquals("비밀번호가 변경 되었습니다!", playerData.lastSentMessage)

        // If password isn't same
        clientCommand.handleMessage("/changepw pass wd", player)
        assertEquals("[scarlet]비밀번호가 같지 않습니다!", playerData.lastSentMessage)
    }

    @Test
    fun clientCommand_chat() {
        // Require admin or adobe permission
        setPermission("admin", true)

        val dummy = newPlayer()

        // Set all players mute
        clientCommand.handleMessage("/chat off", player)
        assertNull(Vars.netServer.chatFormatter.format(dummy.first, "hello"))

        // But if player has chat.admin permission, still can chat
        assertNotNull(Vars.netServer.chatFormatter.format(player, "hello"))

        // Set all players unmute
        clientCommand.handleMessage("/chat on", player)
        assertNotNull(Vars.netServer.chatFormatter.format(dummy.first, "hello"))

        leavePlayer(dummy.first)
    }

    fun clientCommand_chars() {
        // Require admin or adobe permission
        setPermission("admin", true)

        // todo chars 명령어
    }

    @Test
    fun clientCommand_color() {
        // Require admin or adobe permission
        setPermission("admin", true)

        // Enable animated name
        clientCommand.handleMessage("/color", player)
        sleep(1250)
        assertTrue(player.name.contains("[#ff0000]"))

        // Disable animated name
        clientCommand.handleMessage("/color", player)
        sleep(1250)
        assertFalse(player.name.contains("[#ff0000]"))
    }
}