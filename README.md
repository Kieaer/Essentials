# Essentials
Add more commands to the server.  

I'm getting a lot of suggestions.  
Please submit your idea to this repository issues or Mindustry official discord!

## Installation

Put this plugin in the ``<server folder location>/config/plugins`` folder.

## 5.0 Release plans
Many bugs have occurred since the 5.0 update, and I will fix this first.<br>
- [x] Fix network
  - [x] server
  - [x] client
- [x] Fix vote
- [x] Fix server to server (https://github.com/Anuken/Mindustry/pull/896)
  - [x] Update client (It need game client update)
- [ ] Improved detection of griefing

## 5.0 version plans
- [x] Vote
  - [x] Surrender
  - [x] Next wave
  - [x] Ban
- [x] PvP mode rule
  - [ ] ~~Anti coal in mech~~ (Impossible. No event.)
  - [x] Attackable timer
  - [x] Team switching
- [x] Ban share
  - [x] Auto share ban list to other clients
  - [x] ban to sent info
- [x] Cross-chat
  - [x] Show remote IP
- [x] Auto-translate
  - [x] Fix translate not working
- [x] Teleport
  - [x] Detect some nickname
- [ ] Nickname prefix
- [x] Anti-Explode
- [x] Translatable all messages using bundle
- [x] Ranking system
- [x] monitoring message block
  - [ ] Core block
  - [x] Power node

## 6.0 version plans
- [x] Make RPG using player DB
  - [x] Lock blocks
- [ ] Monitoring resource consumption
  - [ ] Alarm when resource consumption is fast
  - [ ] Alarm when resource bank is full
- [ ] AI
  - [ ] Pathfinding
  - [ ] OreFlow
  - [ ] Player
  
## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| tp | &lt;player name&gt; | Teleport to players |
| tpp | &lt;player name&gt; &lt;another player name&gt; | Teleport player to other players |
| me | &lt;msg&gt; | Show special chat format |
| motd |  | Show server motd <br> Can modify from ``config/plugins/Essentials/motd.txt`` |
| getpos |  | Show your current position position |
| info |  | Show player information |
| suicide |  | Kill yourself |
| kill | &lt;player name&gt; | Kill other players |
| time |  | Show server local time |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| kickall |  | Kick all players without you. |
| save |  | Save current map |
| tr |  | Enable/disable auto translate <br> Currently only support Korean to English. |
| ch | &lt;message&gt; | Send chat to another server () <br> You must modify the settings in ``config/plugins/Essentials/config.txt`` |
| status |  | Show currently server status (TPS, RAM, Players/ban count) |
| tempban | &lt;player name&gt; &lt;time&gt; | Temporarily ban player. time unit: 1 hours |
| color |  | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| vote | &lt;gameover/skipwave/kick&gt; [player name] | Enable animated rainbow nickname. <br> Must enable 'realname' and can use admin. |
| login | &lt;account id&gt; &lt;password&gt; | Login to account. |
| register | &lt;account id&gt; &lt;new password&gt; &lt;new password repeat&gt; | Register account |
| spawn | &lt;mob name&gt; &lt;count&gt; &lt;team name&gt; [player name] | Spawn mob in player location |

## Console commands

| Command | Parameter | Description |
|:---|:---|:---|
| ping |  | send ping to remote server |
| tempban | &lt;type-id/ip/name&gt; &lt;player name&gt; &lt;time&gt; | Temporarily ban player. time unit: 1 hours |
| allinfo | &lt;player name&gt; | Show player information |
| bansync |  | Ban list synchronization from master server. <br> Must enable 'banshare' and set master server address. |
| team | &lt;player name&gt;  | Change player team |