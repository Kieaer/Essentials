# Essentials
Add more commands to the server.  

I'm getting a lot of suggestions.  
Please submit your idea to this repository issues or Mindustry official discord!

## Installation

Put this plugin in the ``<server folder location>/config/plugins`` folder.

## Client commands

| Command | Parameter | Description |
|:--|:--|:--|
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
<!--
| vote | &lt;map name&gt; | Vote map |
|  |  |  |
|  |  |  |
-->

## Console commands

| Command | Parameter | Description |
|:--|:--|:--|
| tempban | &lt;type-id/ip/name&gt; &lt;player name&gt; &lt;time&gt; | Temporarily ban player. time unit: 1 hours |
| allinfo | &lt;player name&gt; | Show player information |
| bansync |   | Ban list synchronization from master server. <br> Must enable 'banshare' and set master server address. |

## 4.0 version plans
- [ ] Use all network communications with a single port
  - [x] Ban share
    - [x] Server
    - [x] Client
  - [x] Server as Server chat
    - [x] Server
    - [x] Client
  - [ ] Add server query
    - [ ] Player counting
    - [ ] Player list
    - [ ] Server version
    - [ ] Server description
    - [ ] Server map playtime
    - [ ] difficulty
    - [ ] Core resources status
  - [ ] Discord bot
- [ ] Optimize
  - [ ] Player DB
    - [ ] Read/write source integration
    - [ ] DB Upgrade possible
  - [ ] Config file upgrade possible
  - [ ] All internal plugin log in file

## 5.0 version plans
- [ ] Vote
  - [ ] Surrender
  - [ ] Next wave
  - [ ] Skip many waves
  - [ ] Ban griefer
- [ ] PvP mode rule
  - [ ] Anti coal in mech
  - [ ] Attackable timer
  - [ ] Team switching
- [ ] Ban share
  - [ ] Auto share ban list to other clients
  - [ ] ban to sent info
- [ ] Cross-chat
  - [ ] Show remote IP
- [ ] Auto-translate
  - [ ] Fix translate not working
- [ ] Teleport
  - [ ] using ID
  - [ ] Detect some nickname
- [ ] Nickname prefix
- [ ] Anti-Explode
- [ ] Translatable all messages using bundle

## 6.0 version plans
- [ ] Make RPG using player DB
  - [ ] Lock blocks
- [ ] Monitoring resource consumption
  - [ ] Alarm when resource consumption is fast
  - [ ] Alarm when resource bank is full
