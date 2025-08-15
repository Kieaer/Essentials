package essential.reflection

import arc.Events
import arc.util.Log
import mindustry.Vars
import mindustry.game.EventType
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Reflection-based bridge to read top-level properties and call functions from Essential.jar.
 * Targets essential.PluginDataKt (properties like rootPath, isVoting, players) and essential.util.UtilsKt (functions like findPlayerData).
 *
 * It resolves Essential's ClassLoader and caches the reflected classes/methods for performance.
 */
object EssentialLookup {
    init {
        try {
            Events.on(EventType.ServerLoadEvent::class.java) { _: EventType.ServerLoadEvent ->
                initCache()
            }
        } catch (_: Throwable) { }
    }
    @Volatile
    private var cachedLoader: ClassLoader? = null

    @Volatile
    private var pluginDataKtClass: Class<*>? = null

    @Volatile
    private var utilsKtClass: Class<*>? = null

    private val methodCache: MutableMap<String, Method> = ConcurrentHashMap()

    /** Initialize cache using a known ClassLoader (call from Essential at startup). */
    @JvmStatic
    fun initCacheWithClassLoader(loader: ClassLoader) {
        tryInit(loader)
    }

    /** Initialize cache by trying to resolve Essential's ClassLoader via Mindustry Vars. */
    @JvmStatic
    fun initCache() {
        val loader = resolveEssentialClassLoader() ?: return
        tryInit(loader)
    }

    private fun tryInit(loader: ClassLoader?) {
        if (loader == null) return
        try {
            if (cachedLoader === loader && pluginDataKtClass != null && utilsKtClass != null) return
            val pluginDataClazz = Class.forName("essential.PluginDataKt", true, loader)
            val utilsClazz = Class.forName("essential.util.UtilsKt", true, loader)
            pluginDataKtClass = pluginDataClazz
            utilsKtClass = utilsClazz
            cachedLoader = loader
            methodCache.clear()
        } catch (e: Throwable) {
            Log.err("[EssentialLookup] Failed to initialize reflection cache: @", e)
        }
    }

    /**
     * Attempt to resolve the ClassLoader of the Essential mod.
     */
    private fun resolveEssentialClassLoader(): ClassLoader? {
        return try {
            val mods = Vars.mods ?: return null
            // Prefer official id "Essentials", but fall back to common variants
            mods.getMod("Essentials")?.loader
                ?: mods.getMod("Essential")?.loader
                ?: mods.getMod("essentials")?.loader
                ?: mods.getMod("essential")?.loader
        } catch (e: Throwable) {
            Log.err("[EssentialLookup] Failed to resolve Essential ClassLoader: @", e)
            null
        }
    }

    private fun getMethodFrom(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Method? {
        return try {
            val key = buildString {
                append(clazz.name).append('#').append(name)
                if (paramTypes.isNotEmpty()) {
                    append('(')
                    append(paramTypes.joinToString(",") { it.name })
                    append(')')
                }
            }
            methodCache.getOrPut(key) {
                clazz.getMethod(name, *paramTypes)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getPluginDataMethod(name: String): Method? {
        val clazz = pluginDataKtClass ?: return null
        return getMethodFrom(clazz, name)
    }

    private fun getUtilsMethod(name: String, vararg paramTypes: Class<*>): Method? {
        val clazz = utilsKtClass ?: return null
        return getMethodFrom(clazz, name, *paramTypes)
    }

    /** Generic getter to call a no-arg top-level property getter on PluginDataKt, e.g., getRootPath. */
    @JvmStatic
    fun callNoArg(name: String): Any? {
        return try {
            val m = getPluginDataMethod(name) ?: return null
            m.invoke(null)
        } catch (e: Throwable) {
            Log.err("[EssentialLookup] Failed to call $name: @", e)
            null
        }
    }

    /** rootPath from Essential's PluginData.kt (Fi) */
    @JvmStatic
    fun getRootPath(): arc.files.Fi? {
        val result = callNoArg("getRootPath")
        return result as? arc.files.Fi
    }

    /** isVoting flag from Essential's PluginData.kt */
    @JvmStatic
    fun getIsVoting(): Boolean {
        val result = callNoArg("getIsVoting")
        return (result as? Boolean) ?: false
    }

    /** players list size from Essential's PluginData.kt (avoid cross-loader types) */
    @JvmStatic
    fun getPlayersSize(): Int {
        return try {
            val m = getPluginDataMethod("getPlayers") ?: return 0
            val list = m.invoke(null) as? java.util.List<*>
            list?.size ?: 0
        } catch (_: Throwable) { 0 }
    }

    /** Call essential.util.UtilsKt.findPlayerData(uuid: String) within Essential's classloader. */
    @JvmStatic
    fun findPlayerData(uuid: String): Any? {
        return try {
            val m = getUtilsMethod("findPlayerData", String::class.java) ?: return null
            m.invoke(null, uuid)
        } catch (e: Throwable) {
            Log.err("[EssentialLookup] Failed to invoke findPlayerData: @", e)
            null
        }
    }

    /** Convenience: check if the player exists in Essential's in-memory players list. */
    @JvmStatic
    fun hasPlayerData(uuid: String): Boolean = findPlayerData(uuid) != null

    /** Try to call optional findPlayerDataById(id: Int/UInt) if present; returns Any? */
    @JvmStatic
    fun findPlayerDataById(id: Any): Any? {
        // Try with Int then with UInt (as Kotlin compiles UInt to int in signature due to value class erasure)
        getUtilsMethod("findPlayerDataById", Int::class.java)?.let { m ->
            return runCatching { m.invoke(null, (id as? Number)?.toInt()) }.getOrNull()
        }
        return null
    }

    /** Extract a player's discordID via reflection on PlayerData object returned by Essential's classloader. */
    @JvmStatic
    fun getPlayerDiscordId(uuid: String): String? {
        return try {
            val pd = findPlayerData(uuid) ?: return null
            val getter = pd.javaClass.methods.firstOrNull { it.name == "getDiscordID" && it.parameterCount == 0 }
                ?: pd.javaClass.methods.firstOrNull { it.name.equals("getDiscordId", true) && it.parameterCount == 0 }
            getter?.invoke(pd) as? String
        } catch (e: Throwable) {
            Log.err("[EssentialLookup] Failed to read PlayerData.discordID: @", e)
            null
        }
    }
}
