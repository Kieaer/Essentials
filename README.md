# Essentials
Add more commands to the server.

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Requirements for running this plugin
This plugin does a lot of disk read/write operations depending on the features usage.

### Minimum
CPU: Athlon 200GE or Intel i5 2300<br>
RAM: 20MB<br>
Disk: HDD capable of more than 2MB/s random read/write.

### Recommand
CPU: Ryzen 3 2200G or Intel i3 8100<br>
RAM: 50MB<br>
Disk: HDD capable of more than 5MB/s random read/write.

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.

## Essentials 9.0 Plans
- [ ] Server
  - [ ] Control server using Web server
- [x] PlayerDB
  - [x] DB Server without MySQL/MariaDB..
- [x] Internal
  - [x] Fix server can't shutdown
  - [x] Make ban reason
- [ ] Anti-grief
  - [ ] Make anti-filter art
  - [ ] Make anti-fast build/break destroy
  - [ ] Make detect custom client
- [ ] Soruce code rebuild (working [remake branch](https://github.com/Kieaer/Essentials/tree/remake))
  - [ ] core
    - [x] Discord
    - [x] PlayerDB
  - [x] net
    - [x] Client
    - [x] Server
  - [ ] special
    - [ ] DataMigration
    - [x] DriverLoader
  - [x] utils
    - [x] Bundle
    - [x] Config
  - [x] Global
  - [ ] Main
  - [x] Threads
    - [x] login
    - [x] changename
    - [x] AutoRollback
    - [x] eventserver
    - [x] ColorNick
    - [x] monitorresource
    - [x] Vote
    - [x] jumpdata
    - [x] visualjump

## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| alert |  | Turn on/off alerts |
| ch |  | Send chat to another server. |
| changepw | &lt;new_password&gt; | Change account password |
| chars | &lt;Text...&gt; | Make pixel texts |
| color |  | Enable color nickname |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| killall |  | Kill all enemy units |
| event | &lt;host/join&gt; &lt;roomname&gt; [map] [gamemode] | Host your own server |
| email | &lt;key&gt; | Email Authentication |
| info |  | Show your information |
| jump | &lt;zone/count/total&gt; &lt;touch&gt; [serverip] [range] | Create a server-to-server jumping zone. |
| kickall |  | Kick all players |
| kill | &lt;player&gt; | Kill player. |
| login | &lt;id&gt; &lt;password&gt; | Access your account |
| logout |  | Log-out of your account. |
| maps | [page] | Show server maps |
| me | &lt;text...&gt; | broadcast * message |
| motd |  | Show server motd. |
| players |  | Show players list |
| save |  | Auto rollback map early save |
| reset | &lt;zone/count/total&gt; [ip] | Remove a server-to-server jumping zone data. |
| register | &lt;accountid&gt; &lt;password&gt; | Register account |
| spawn | &lt;mob_name&gt; &lt;count&gt; [team] [playername] | Spawn mob in player position |
| setperm | &lt;player_name&gt; &lt;group&gt; | Set player permission |
| spawn-core | &lt;smail/normal/big&gt; | Make new core |
| setmech | &lt;Mech&gt; [player] | Set player mech |
| status |  | Show server status |
| suicide |  | Kill yourself. |
| team | [Team...] | Change team (PvP only) |
| tempban | &lt;player&gt; &lt;time&gt; &lt;reason&gt; | Temporarily ban player. time unit: 1 hours |
| time |  | Show server time |
| tp | &lt;player&gt; | Teleport to other players |
| tpp | &lt;player&gt; &lt;player&gt; | Teleport to other players |
| tppos | &lt;x&gt; &lt;y&gt; | Teleport to coordinates |
| tr |  | Enable/disable Translate all chat |
| vote | &lt;mode&gt; [parameter...] | Voting system (Use /vote to check detail commands) |
| weather | &lt;day,eday,night,enight&gt; | Change map light |
| mute | &lt;Player_name&gt; | Mute/unmute player |
| votekick | [player_name] | Player kick starts voting. |

## Server commands

| Command | Parameter | Description |
|:---|:---|:--- |
| pardon | &lt;ID&gt; | Pardons a votekicked player by ID and allows them to join again. |
| players |  | List all players currently in game. |
| gendocs |  | Generate Essentials README.md |
| allinfo | &lt;name&gt; | Show player information. |
| bansync |  | Ban list synchronization from main server. |
| banreason | &lt;name...&gt; | Show player ban reason |
| blacklist | &lt;add/remove&gt; &lt;nickname&gt; | Block special nickname. |
| reset | &lt;zone/count/total&gt; | Clear a server-to-server jumping zone data. |
| restart |  | Plugin restart |
| unadminall | &lt;default_group_name&gt; | Remove all player admin status |
| kickall |  | Kick all players. |
| kill | &lt;username&gt; | Kill target player. |
| mute | &lt;Player_name&gt; | Mute/unmute player |
| nick | &lt;name&gt; &lt;newname...&gt; | Set player nickname |
| pvp | &lt;anticoal/timer&gt; [time...] | Set gamerule with PvP mode. |
| setperm | &lt;player_name&gt; &lt;group&gt; | Set player permission group |
| sync | &lt;player&gt; | Force sync request from the target player. |
| team | &lt;name&gt; | Change target player team. |
| tempban | &lt;player_name&gt; &lt;time&gt; &lt;reason&gt; | Temporarily ban player. time unit: 1 hours. |

README.md Generated time: 2020-03-17 21:28:09
