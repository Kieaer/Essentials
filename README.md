# Essentials
Add more commands to the server.  

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Requirements for running this plugin
This plugin does a lot of disk read/write operations depending on the features usage.

### Minimum
CPU: Athlon 200GE or Intel i5 2300<br>
RAM: 256MB<br>
Disk: HDD capable of more than 3MB/s random read/write.

### Recommand
CPU: Ryzen 3 2200G or Intel i3 8100<br>
RAM: 500MB<br>
Disk: SSD capable of more than 10MB/s random read/write.

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.
  
## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| ch | &lt;massage&gt; | Send chat to another server <br> You must modify the settings in ``config/plugins/Essentials/config.hjson`` |
| changepw | &lt;new password&gt; | Change account password |
| color |  | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| despawn |  | Kill all units ||
| event | &lt;host/join&gt; &lt;roomname&gt; [map] [gamemode] | Host your own server |
| getpos |  | Show your current position position |
| help | [page] | Show command lists |
| info |  | Show player information |
| jump | &lt;zone/count/total&gt; [serverip] [port] [range] [block-type(1~6)] | Create a server-to-server jumping zone. |
| kickall |  | Kick all players |
| kill | &lt;player name&gt; | Kill other players |
| login | &lt;account id&gt; &lt;password&gt; | Login to account. |
| logout |  | Log out of my account |
| maps | [page] |  Show server maps |
| me | &lt;msg&gt; | Show special chat format |
| motd |  | Show server motd <br> Can modify from ``config/plugins/Essentials/motd.txt`` |
| register | &lt;account id&gt; &lt;new password&gt; | Register accoun<br>Example - ``/register test test123`` |
| save |  | Save current map |
| spawn | &lt;mob name&gt; &lt;count&gt; &lt;team name&gt; [name] | Spawn mob in player location |
| setperm | &lt;player name&gt; &lt;group name&gt; | Set player permission |
| status |  | Show currently server status (TPS, RAM, Players/ban count) |
| suicide |  | Kill yourself |
| team | [Team...] | Change team (PvP only) |
| tempban | &lt;player name&gt; &lt;time&gt; | Temporarily ban player. time unit: 1 hours |
| time |  | Show server local time |
| tpp | &lt;player name&gt; &lt;another player name&gt; | Teleport player to other players |
| tp | &lt;player name&gt; | Teleport to players |
| tr |  | Enable/disable auto translate <br> Currently only support Korean to English. |
| vote | &lt;gameover/skipwave/kick/rollback/map&gt; [name] | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| votekick | &lt;player name&gt; | Player kick starts voting |

## Console commands

| Command | Parameter | Description |
|:---|:---|:---|
| accountban | &lt;ban/unban&gt; &lt;account_uuid&gt; | Ban player account |
| admin | &lt;player name&gt; | **!! Disabled command !!** |
| allinfo | &lt;player name&gt; | Show player information |
| ban | &lt;uuid/name/ip&gt; &lt;username/ip/uuid&gt; | Ban a person. |
| bansync |  | Ban list synchronization from main server |
| blacklist | &lt;add/remove&gt; &lt;player name&gt; | Block special nickname. |
| reset | &lt;zone/count/total&gt; | Clear a server-to-server jumping zone data. |
| reload |  | Reload Essentials config |
| reconnect |  | Reconnect remote server (Essentials server only!) |
| kickall |  | Kick all players |
| kill | &lt;player name&gt; | Kill target player |
| nick | &lt;player name&gt; &lt;new name&gt; | Show player information |
| pvp | &lt;anticoal/timer&gt; [time...] | Set gamerule with PvP mode |
| setperm | &lt;player name&gt; &lt;group name&gt; | Set player permission group |
| sync | &lt;player name&gt; | Force sync request from the target player |
| team | &lt;player name&gt; | Change target player team |
| tempban | &lt;uuid/name/ip&gt; &lt;username/ip/uuid&gt; | Temporarily ban player. time unit: 1 hours |
