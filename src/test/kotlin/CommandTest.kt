import PluginTest.Companion.clientCommand
import PluginTest.Companion.path
import PluginTest.Companion.player
import arc.Core
import arc.Events
import essentials.DB
import essentials.Main.Companion.database
import essentials.Permission
import junit.framework.TestCase.assertEquals
import mindustry.Vars
import mindustry.game.EventType.PlayerJoin
import mindustry.game.Gamemode
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.junit.AfterClass
import org.junit.Test
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

    init {
        val test = PluginTest()
        test.loadGame()
        test.loadPlugin()
        test.runPost()

        Events.fire(PlayerJoin(player.self()))

        // Wait for database register time
        sleep(500)

        playerData = database.players.find { data -> data.uuid == player.uuid() }
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
}