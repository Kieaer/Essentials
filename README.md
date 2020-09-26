[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Kieaer_Essentials) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/dashboard?id=Kieaer_Essentials)
# Essentials
Add more commands to the server.

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Essentials 11 Plans
- [ ] Fix bugs
  - [ ] Voting not working
  - [ ] Sometimes an account system not working
- [ ] Fix many typos
- [ ] Features separation
  - [x] Rest API [Plugin Link](https://github.com/Kieaer/Essential-REST_API)
    - [x] Information
      - [x] Add players detail information
      - [x] Add a gamemode
      - [x] Add other team core resource status
      - [x] Add a server map list
  - [ ] Web server
    - [ ] Fix a sometimes ranking site not loaded
  - [x] Auto Rollback (Not remove) [Plugin Link](https://github.com/Kieaer/AutoRollback)
- [ ] New features
  - [ ] Web console
    - [ ] Control plugin database
    - [ ] Check world status
      - [ ] Dynmap (idea from Minecraft)
      - [ ] Rest API
- [ ] Remove external API services
  - [x] IP API (Due to traffic excess)
  - [x] Translate (Due to paid service)
- [ ] Security patches
- [ ] All code clean

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| alert |  | Turn on/off alerts |
| ch |  | Send chat to another server. |
| changepw | &lt;new_password&gt; &lt;new_password_repeat&gt; | Change account password |
| chars | &lt;Text...&gt; | Make pixel texts |
| color |  | Enable color nickname |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| killall |  | Kill all enemy units |
| event | &lt;host/join&gt; &lt;roomname&gt; [map] [gamemode] | Host your own server |
| info |  | Show your information |
| warp | &lt;zone/block/count/total&gt; [ip] [parameters...] | Create a server-to-server warp zone. |
| kickall |  | Kick all players |
| kill | [player] | Kill player. |
| login | &lt;id&gt; &lt;password&gt; | Access your account |
| logout |  | Log-out of your account. |
| maps | [page] | Show server maps |
| me | &lt;text...&gt; | broadcast * message |
| motd |  | Show server motd. |
| save |  | Auto rollback map early save |
| r | &lt;player&gt; [message] | Send Direct message to target player |
| reset | &lt;zone/count/total/block&gt; [ip] | Remove a server-to-server warp zone data. |
| router |  | Router |
| register | &lt;accountid&gt; &lt;password&gt; | Register account |
| spawn | &lt;mob_name&gt; &lt;count&gt; [team] [playerName] | Spawn mob in player position |
| setperm | &lt;player_name&gt; &lt;group&gt; | Set player permission |
| spawn-core | &lt;smail/normal/big&gt; | Make new core |
| setmech | &lt;Mech&gt; [player] | Set player mech |
| status |  | Show server status |
| suicide |  | Kill yourself. |
| team | &lt;team_name&gt; | Change team |
| tempban | &lt;player&gt; &lt;time&gt; &lt;reason&gt; | Temporarily ban player. time unit: 1 minute |
| time |  | Show server time |
| tp | &lt;player&gt; | Teleport to other players |
| tpp | &lt;source&gt; &lt;target&gt; | Teleport to other players |
| tppos | &lt;x&gt; &lt;y&gt; | Teleport to coordinates |
| vote | &lt;mode&gt; [parameter...] | Voting system (Use /vote to check detail commands) |
| weather | &lt;day/eday/night/enight&gt; | Change map light |
| mute | &lt;Player_name&gt; | Mute/unmute player |

## Server commands

| Command | Parameter | Description |
|:---|:---|:--- |
| lobby |  | Toggle lobby server features |
| edit | &lt;uuid&gt; &lt;name&gt; [value] | Edit PlayerData directly |
| saveall |  | desc |
| bansync |  | Synchronize ban list with server |
| setperm | &lt;player_name/uuid&gt; &lt;group&gt; | Set player permission |
| reload |  | Reload Essential plugin data |

README.md Generated time: 2020-07-29 23:09:08