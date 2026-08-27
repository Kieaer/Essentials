package essential.core

import arc.util.CommandHandler
import arc.util.Log
import arc.util.Timer
import essential.common.database.data.PlayerData
import mindustry.mod.Plugin

/**
 * Loads optional services without linking the core plugin to their classes.
 *
 * A modular artifact omits service classes at build time. Keeping the optional
 * boundary here means that an omitted service is treated as unavailable rather
 * than causing [NoClassDefFoundError] while Essentials is starting.
 */
object ModuleRuntime {
    private data class ServiceDescriptor(
        val id: String,
        val implementationClass: String,
        val enabled: () -> Boolean,
    )

    private val serviceDescriptors = listOf(
        ServiceDescriptor("bridge", "essential.core.service.bridge.BridgeService") { Main.conf.module.bridge },
        ServiceDescriptor("chat", "essential.core.service.chat.ChatService") { Main.conf.module.chat },
        ServiceDescriptor("protect", "essential.core.service.protect.ProtectService") { Main.conf.module.protect },
        ServiceDescriptor("achievements", "essential.core.service.achievements.AchievementService") { Main.conf.module.achievement },
        ServiceDescriptor("contribution", "essential.core.service.contribution.ContributionService") { Main.conf.module.contribution },
        ServiceDescriptor("discord", "essential.core.service.discord.DiscordService") { Main.conf.module.discord },
        ServiceDescriptor("web", "essential.core.service.web.WebService") { Main.conf.module.web },
    )

    private val loadedServices = mutableMapOf<String, Plugin>()

    fun initEnabledServices() {
        serviceDescriptors
            .filter { it.enabled() }
            .forEach { descriptor -> service(descriptor)?.init() }
    }

    fun registerServerCommands(handler: CommandHandler) {
        serviceDescriptors
            .filter { it.enabled() }
            .forEach { descriptor -> service(descriptor)?.registerServerCommands(handler) }
    }

    fun registerClientCommands(handler: CommandHandler) {
        serviceDescriptors
            .filter { it.enabled() }
            .forEach { descriptor -> service(descriptor)?.registerClientCommands(handler) }
    }

    fun reloadEnabledConfigurations() {
        serviceDescriptors
            .filter { it.enabled() }
            .forEach { descriptor -> invokeCompanion(descriptor.implementationClass, "reloadConf") }
    }

    fun processPlayerDataLoad(playerData: PlayerData) {
        invokeObject("essential.core.service.achievements.AchievementHooks", "processPlayerDataLoad", playerData)
    }

    fun scheduleLevelEffects() {
        val task = instantiate("essential.core.service.effect.EffectSystem") as? Timer.Task ?: return
        Timer.schedule(task, 0f, 0.05f)
    }

    fun startVote(voteData: VoteData): Boolean {
        val task = try {
            Class.forName("essential.core.service.vote.VoteSystem")
                .getConstructor(VoteData::class.java)
                .newInstance(voteData) as? Timer.Task
        } catch (_: ClassNotFoundException) {
            null
        } catch (e: ReflectiveOperationException) {
            Log.err("Failed to start the optional vote service", e)
            null
        }

        if (task == null) return false
        Timer.schedule(task, 0f, 1f, 60)
        return true
    }

    fun isPasswordAuthenticationEnabled(): Boolean =
        (invokeCompanion("essential.core.service.protect.ProtectService", "isPasswordAuthenticationEnabled") as? Boolean)
            ?: false

    fun awardVotingBan(playerData: PlayerData) {
        invokeObject("essential.core.service.achievements.AchievementHooks", "awardVotingBan", playerData)
    }

    private fun service(descriptor: ServiceDescriptor): Plugin? = loadedServices[descriptor.id] ?: try {
        Class.forName(descriptor.implementationClass)
            .getDeclaredConstructor()
            .newInstance() as Plugin
    } catch (_: ClassNotFoundException) {
        null
    } catch (e: ReflectiveOperationException) {
        Log.err("Failed to load optional ${descriptor.id} service", e)
        null
    }?.also { loadedServices[descriptor.id] = it }

    private fun instantiate(className: String): Any? = try {
        Class.forName(className).getDeclaredConstructor().newInstance()
    } catch (_: ClassNotFoundException) {
        null
    } catch (e: ReflectiveOperationException) {
        Log.err("Failed to load optional class $className", e)
        null
    }

    private fun invokeCompanion(className: String, methodName: String): Any? = try {
        val implementationClass = Class.forName(className)
        val companion = implementationClass.getField("Companion").get(null)
        companion.javaClass.getMethod(methodName).invoke(companion)
    } catch (_: ClassNotFoundException) {
        null
    } catch (e: ReflectiveOperationException) {
        Log.err("Failed to invoke optional service method $className.$methodName", e)
        null
    }

    private fun invokeObject(className: String, methodName: String, argument: Any): Any? = try {
        val objectClass = Class.forName(className)
        val instance = objectClass.getField("INSTANCE").get(null)
        objectClass.methods
            .firstOrNull { method -> method.name == methodName && method.parameterCount == 1 }
            ?.invoke(instance, argument)
    } catch (_: ClassNotFoundException) {
        null
    } catch (e: ReflectiveOperationException) {
        Log.err("Failed to invoke optional service method $className.$methodName", e)
        null
    }
}
