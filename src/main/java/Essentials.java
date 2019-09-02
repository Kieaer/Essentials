package essential;

import io.anuke.arc.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.plugin.Plugin;
import io.anuke.mindustry.net.Packets.KickReason;

public class Essentials extends Plugin{
    public Essentials(){
        
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("motd", "Show server motd.", (args, player) -> {
        	Core.settings.getDataDirectory().child("motd.txt").writeString("");
        	String motd = Core.settings.getDataDirectory().child("motd.txt").readString();
        	player.sendMessage(motd);
        });

        handler.<Player>register("getpos", "Get your current position info", (args, player) -> {
        	player.sendMessage("X: " + Math.round(player.x) + " Y: " + Math.round(player.y));
        });

        handler.<Player>register("info", "Show your information", (args, player) -> {
        	player.sendMessage("[green]Name[]: " + player.name);
        	player.sendMessage("[green]UUID[]: " + player.uuid);
        	player.sendMessage("[green]Mobile[]: " + player.isMobile);
        	player.sendMessage("[green]lastText[]: " + player.lastText);
        });

        // Teleport source from https://github.com/J-VdS/locationplugin
        handler.<Player>register("tp", "<player...>", "Teleport to other players", (args, player) -> {
        	Player other = Vars.playerGroup.find(p->p.name.equalsIgnoreCase(args[0]));
        	if(other == null){
                player.sendMessage("[scarlet]No player by that name found!");
                return;
            }
        	if(player.isAdmin == false){
        		player.sendMessage("[green]Notice:[] You're not admin!");
        	} else {
        		if(other == null){
            	    player.sendMessage("[scarlet]No player by that name found!");
            	    return;
            	}
        		player.setNet(other.x, other.y);
        	}
        });
        

        handler.<Player>register("kickall", "Kick all players", (args, player) -> {
        	if(player.isAdmin == false){
        		player.sendMessage("[green]Notice: [] You're not admin!");
        	} else {
        		// Call.onKick(other, KickReason.kick);
        	}
        });

        handler.<Player>register("spawnmob", "Spawn mob", (args, player) -> {
        	if(player.isAdmin == false){
        		player.sendMessage("[green]Notice: [] You're not admin!");
        	} else {
        		player.sendMessage("source here");
        	}
        });

        handler.<Player>register("tempban", "timer ban", (args, player) -> {
        	if(player.isAdmin == false){
        		player.sendMessage("[green]Notice: [] You're not admin!");
        	} else {
        		player.sendMessage("source here");
        	}
        });

        handler.<Player>register("me", "<text...>", "broadcast * message", (args, player) -> {
        	Call.sendMessage("[orange]*[] " + player.name + "[orange][] : " + args[0]);
        });

        handler.<Player>register("difficulty", "<difficulty...>", "Set server difficulty", (args, player) -> {
        	//source
        });

        handler.<Player>register("effect", "<effect...>", "make effect", (args, player) -> {
        	//source
        });

        handler.<Player>register("gamerule", "<gamerule...>", "Set gamerule", (args, player) -> {
        	//source
        });

        handler.<Player>register("vote", "<vote...>", "Votemap", (args, player) -> {
        	//source
        });

        handler.<Player>register("kill", "<player...>", "Kill player", (args, player) -> {
        	//source
        });

        handler.<Player>register("save", "Map save", (args, player) -> {
        	//source
        });

        handler.<Player>register("time", "Show server time", (args, player) -> {
        	//source
        });

        handler.<Player>register("team", "<team...>", "Set team", (args, player) -> {
        	//source
        });

        handler.<Player>register("banlist", "Show banlist", (args, player) -> {
        	//source
        });
    }
}

/*
+ Kickall - 전체 강퇴
+ tp playername playername - 플레이어에게 이동
+ spawnmob mob amount - 몹 스폰
status - 서버 상태
realname player - 실제 이름표시
// me msg - * 메세지로 대화
firework player - 플레이어 폭죽 표시
// motd - 서버 motd 표시
// getpos - 현재 위치 표시 
tempban player time - 일정 시간동안 밴
//info - 플레이어 정보 표시
+ difficulty - 난이도 설정
+ effect - effect 효과
+ gamerule - 게임 rule 설정
+ vote - 맵 투표
+ kill - 자폭
+ save - 맵 저장
say - 서버 알림
nick - 닉네임 설정
+ time - 서버 시간 표시
+ team - PvP Team 설정
+ banlist - ban 목록 표시
*/

