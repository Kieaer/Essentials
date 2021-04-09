package essentials

class PluginTest {
    /*companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            val testMap = arrayOfNulls<Map>(1)
            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                val core: ApplicationCore = object : ApplicationCore() {
                    override fun setup() {
                        Vars.headless = true
                        Vars.net = Net(null)
                        Vars.tree = FileTree()
                        Vars.init()
                        Vars.content.createBaseContent()
                        add(Logic().also { Vars.logic = it })
                        add(NetServer().also { Vars.netServer = it })
                        Vars.content.init()
                    }

                    override fun init() {
                        super.init()
                        begins[0] = true
                        testMap[0] = Vars.maps.loadInternalMap("maze")
                        Thread.currentThread().interrupt()
                    }
                }
                HeadlessApplication(core, 60f) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        exceptionThrown[0]!!.printStackTrace()
                        fail()
                    }
                    sleep(10)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @AfterClass
        @JvmStatic
        fun shutdown() {
            Core.app.listeners[0].dispose()
            Client.request(Client.Request.Exit, null, null)
            Server.shutdown()
        }
    }*/
}