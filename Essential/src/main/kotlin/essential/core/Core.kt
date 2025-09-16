package essential.core

import essential.common.event.CustomEvents
import mindustry.game.EventType.*

/**
 * This class provides static access to the event handler methods defined in CoreEvent.kt.
 * It's used by the generated code to call the top-level functions.
 */
object Core {
    // Event handlers for various event types
    @JvmStatic
    fun withdraw(event: WithdrawEvent) = essential.core.withdraw(event)

    @JvmStatic
    fun deposit(event: DepositEvent) = essential.core.deposit(event)

    @JvmStatic
    fun config(event: ConfigEvent) = essential.core.config(event)

    @JvmStatic
    fun tap(event: TapEvent) = essential.core.tap(event)

    @JvmStatic
    fun wave(event: WaveEvent) = essential.core.wave(event)

    @JvmStatic
    fun serverLoad(event: ServerLoadEvent) = essential.core.serverLoad(event)

    @JvmStatic
    fun gameOver(event: GameOverEvent) = essential.core.gameOver(event)

    @JvmStatic
    fun blockBuildBegin(event: BlockBuildBeginEvent) = essential.core.blockBuildBegin(event)

    @JvmStatic
    fun blockBuildEnd(event: BlockBuildEndEvent) = essential.core.blockBuildEnd(event)

    @JvmStatic
    fun buildSelect(event: BuildSelectEvent) = essential.core.buildSelect(event)

    @JvmStatic
    fun blockDestroy(event: BlockDestroyEvent) = essential.core.blockDestroy(event)

    @JvmStatic
    fun unitDestroy(event: UnitDestroyEvent) = essential.core.unitDestroy(event)

    @JvmStatic
    fun unitCreate(event: UnitCreateEvent) = essential.core.unitCreate(event)

    @JvmStatic
    fun playerJoin(event: PlayerJoin) = essential.core.playerJoin(event)

    @JvmStatic
    fun playerLeave(event: PlayerLeave) = essential.core.playerLeave(event)

    @JvmStatic
    fun playerBan(event: PlayerBanEvent) = essential.core.playerBan(event)

    @JvmStatic
    fun playerUnban(event: PlayerUnbanEvent) = essential.core.playerUnban(event)

    @JvmStatic
    fun playerIpUnban(event: PlayerIpUnbanEvent) = essential.core.playerIpUnban(event)

    @JvmStatic
    fun worldLoad(event: WorldLoadEvent) = essential.core.worldLoad(event)

    @JvmStatic
    fun connectPacket(event: ConnectPacketEvent) = essential.core.connectPacket(event)

    @JvmStatic
    fun playerConnect(event: PlayerConnect) = essential.core.playerConnect(event)

    @JvmStatic
    fun buildingBulletDestroy(event: BuildingBulletDestroyEvent) = essential.core.buildingBulletDestroy(event)

    @JvmStatic
    fun configFileModified(event: CustomEvents.ConfigFileModified) = essential.core.configFileModified(event)
}