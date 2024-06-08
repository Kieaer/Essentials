package essentials.core

import arc.util.CommandHandler
import arc.util.Log
import mindustry.mod.Plugin

class Main : Plugin() {
    override fun init() {
        Log.info("")
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        super.registerServerCommands(handler)
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        super.registerClientCommands(handler)
    }
}