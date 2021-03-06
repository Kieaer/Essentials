package essentials.event

import arc.Events
import mindustry.game.EventType.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object Event {
    fun register() {
        // 플레이어가 블록에 대해 어떠한 설정을 했을 때 작동
        Events.on(ConfigEvent::class.java) {
            EventThread(EventThread.EventTypes.Config, it).run()
        }

        // 플레이어가 블록을 터치하거나 클릭했을 때 작동
        Events.on(TapEvent::class.java) {
            EventThread(EventThread.EventTypes.Tap, it).run()
        }

        // 게임오버가 되었을 때 작동
        Events.on(GameOverEvent::class.java) {
            EventThread(EventThread.EventTypes.Gameover, it).run()
        }

        Events.on(WorldLoadEvent::class.java) {
            EventThread(EventThread.EventTypes.WorldLoad, it).run()
        }

        Events.on(PlayerConnect::class.java) {
            EventThread(EventThread.EventTypes.PlayerConnect, it).run()
        }

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때 작동
        Events.on(DepositEvent::class.java) {
            EventThread(EventThread.EventTypes.Deposit, it).run()
        }

        // 플레이어가 서버에 들어왔을 때 작동
        Events.on(PlayerJoin::class.java) {
            EventThread(EventThread.EventTypes.PlayerJoin, it).run()
        }

        // 플레이어가 서버에서 나갔을 때 작동
        Events.on(PlayerLeave::class.java) {
            EventThread(EventThread.EventTypes.PlayerLeave, it).run()
        }

        // 플레이어가 채팅을 했을 때 작동
        Events.on(PlayerChatEvent::class.java) {
            EventThread(EventThread.EventTypes.PlayerChat, it).run()
        }

        // 플레이어가 블럭 건설을 끝마쳤을 때 작동
        Events.on(BlockBuildEndEvent::class.java) {
            EventThread(EventThread.EventTypes.BlockBuildEnd, it).run()
        }

        // 플레이어가 블럭을 선택했을 때 작동 (파괴 등)
        Events.on(BuildSelectEvent::class.java) {
            EventThread(EventThread.EventTypes.BuildSelect, it).run()
        }

        // 종류 상관없이 유닛이 파괴되었을 때 작동
        Events.on(UnitDestroyEvent::class.java) {
            EventThread(EventThread.EventTypes.UnitDestroy, it).run()
        }

        // 플레이어가 차단되었을 때 작동
        Events.on(PlayerBanEvent::class.java) {
            EventThread(EventThread.EventTypes.PlayerBan, it).run()
        }

        // 플레이어가 IP 차단되었을 때 작동
        Events.on(PlayerIpBanEvent::class.java) {
            EventThread(EventThread.EventTypes.PlayerIpBan, it).run()
        }

        // 플레이어가 차단 해제되었을 때 작동
        Events.on(PlayerUnbanEvent::class.java) {
            EventThread(EventThread.EventTypes.PlayerUnban, it).run()
        }

        // 플레이어의 IP 차단이 해제되었을 때 작동
        Events.on(PlayerIpUnbanEvent::class.java) {
            EventThread(EventThread.EventTypes.PlayerIpUnban, it).run()
        }

        // 서버가 시작되었을 때 작동
        Events.on(ServerLoadEvent::class.java) {
            EventThread(EventThread.EventTypes.ServerLoaded, it).run()
        }
    }
}