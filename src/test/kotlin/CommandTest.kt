import PluginTest.Companion.clientCommand
import PluginTest.Companion.player
import arc.Core
import essentials.Permission
import mindustry.Vars
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.junit.Test

class CommandTest {
    init {
        val test = PluginTest()
        test.loadGame()
        test.loadPlugin()
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
        setPermission("owner", true)

        // if map not found
        val oldMap = Vars.state.map.name()
        clientCommand.handleMessage("/changemap nothing survival", player)
        assert(Vars.state.map.name() == oldMap)

        // number method
        clientCommand.handleMessage("/changemap 0 survival", player)
        assert(Vars.state.map.name() == "Ancient Caldera")

        // name method
        clientCommand.handleMessage("/changemap fork survival", player)
        assert(Vars.state.map.name() == "Fork")
    }
}