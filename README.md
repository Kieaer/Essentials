# Essentials
Add more commands to the server.  
It's under development!

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
| allinfo | %lt;player name&gt; | Show player information |

## Pinned plan

- [x] Kickall - Kick all players.
- [x] Teleport
  - [x] tp playername playername - Teleport from Player to Player
  - [x] tp playername - Teleport to Player.
- [x] status - Show server status.
- [x] realname player - Show real name.
  - [x] make info command (dependence)
- [x] me msg - Special chat format
- [x] motd - Show server motd.
- [x] getpos - Show current position.
- [x] tempban player time - Timer ban.
  - [x] make info command (dependence)
- [x] info - Show player info.
  - [x] Player name and UUID
  - [x] Show IP and GeoLocation
  - [x] Show destroy/placed block count
  - [x] Show destroy enemies count
  - [x] Show dead count
  - [x] Rank system
- [x] difficulty - Set difficulty.
- [ ] vote - Map vote.
  - [ ] Map list
  - [x] surrender vote (NOT TESTED)
- [x] suicide - self-destruct.
- [x] kill - Kill other player.
- [x] save - Map save.
- [ ] say - Server chat.
- [x] ch - Cross server chat.
- [x] time - Show server time.
- [x] translate - Enable/disable auto translate
