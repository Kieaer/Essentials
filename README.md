# Essentials
Add more commands to the server.  

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Requirements for running this plugin
This plugin does a lot of disk read/write operations depending on the features usage.

### Minimum
CPU: Ryzen 3 2200G or Intel i3 8100<br>
RAM: 256MB<br>
Disk: A disk capable of reading and writing normally at speeds above 10MB/s.

### Recommand
CPU: Ryzen 5 2600x or Intel i5 8500<br>
RAM: 500MB<br>
Disk: SSD capable of more than 30MB/s random read/write.

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.

## 7.0 version plans
- [ ] Plugin performance optimizing <img src="https://preloaders.evidweb.com/d_file.php?file=images/preloaders/squares.gif">
- [ ] Monitoring resource consumption
  - [ ] Alarm when resource bank is full
- [ ] AI
  - [ ] Pathfinding
  - [ ] OreFlow
  - [ ] Player
- [x] Improved detection of griefing
  - [x] Detect non-block destroy/place griefing
    - [x] ALL WithDraw events
- [ ] Make lobby features
  - [ ] Show all server players
    - [ ] massage block 
  - [ ] Show client server players
    - [ ] massage block
- [ ] monitoring massage block
  - [ ] Core block
- [ ] PvP mode rule
  - [ ] Anti coal in mech
- [ ] Chat feature upgrade
  - [ ] Blocking bad words
  - [ ] Nickname prefix
  - [ ] Remove translate before massage
  - [ ] PvP Team chat without command
- [ ] Improved rank system
  - [ ] Show personal ranking record
  
## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| ch | &lt;massage&gt; | Send chat to another server () <br> You must modify the settings in ``config/plugins/Essentials/config.txt`` |
| changepw | &lt;new password&gt; | Change account password |
| color |  | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
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
| register | &lt;account id&gt; &lt;new password&gt; &lt;new password repeat&gt; | Register accoun<br>Example - ``/register test test123 test123`` |
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
