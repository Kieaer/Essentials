# Essentials
Add more commands to the server.  
It's under development!

I'm getting a lot of suggestions.  
Please submit your idea to this repository issues or Mindustry official discord!

## Installation

Put this plugin in the ``<server folder location>/config/plugins`` folder.

## Commands

| Command | Parameter | Description |
|:--|:--|:--|
| tp | &lt;player name&gt; | Teleport to players |
| tp | &lt;player name&gt; &lt;another player name&gt; | Teleport player to other players |
| me | &lt;msg&gt; | Show special chat format |
| motd |  | Show server motd <br> Can modify from ``config/plugins/Essentials/motd.txt`` |
| getpos |  | Show your current position position |
| info |  | Show player information |
| suicide |  | Kill yourself |
| kill | &lt;player name&gt; | Kill other players |
| time |  | Show server local time |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| kickall |  | Kick all players without you. |
| difficulty | &lt;difficulty&gt; | Set server difficulty |
| save |  | Save current map |
| tr |  | Enable/disable auto translate <br> Currently only support Korean to English. |
| ch | &lt;message&gt; | Send chat to another server () <br> You must modify the settings in ``config/plugins/Essentials/config.txt`` |
<!--
| tempban |  | Temporarily ban player. time unit: 1 hours |
| spawnmob |  | Spawn mob |
| effect |  | make effect |
| gamerule |  | Edit gamerule |
| vote | &lt;map name&gt; | Vote map |
| ch | msg | Cross server chat |
| tpmouse | player | teleport player to follow mouse pointer |
|  |  |  |
|  |  |  |
-->

## Pinned plan

- [x] Kickall - Kick all players.
  - [ ] ~~Exclude who entered the command~~
- [x] Teleport
  - [x] tp playername playername - Teleport from Player to Player
  - [x] tp playername - Teleport to Player.
- [ ] spawnmob mob amount - Spawn enemies or mob.
- [x] status - Show server status.
- [ ] realname player - Show real name.
  - [x] make info command (dependence)
- [x] me msg - Special chat format
- [x] motd - Show server motd.
- [x] getpos - Show current position.
- [ ] tempban player time - Timer ban.
  - [x] make info command (dependence)
- [ ] info - Show player info.
  - [x] Player name and UUID
  - [x] Show IP and GeoLocation
  - [x] Show destroy/placed block count
  - [x] Show destroy enemies count
  - [] Show dead count
  - [ ] Rank system
- [x] difficulty - Set difficulty.
- [ ] effect - Make effect.
- [ ] gamerule - Set gamerule.
- [ ] vote - Map vote.
  - [ ] Map list
- [x] suicide - self-destruct.
- [x] kill - Kill other player.
- [x] save - Map save.
- [ ] say - Server chat.
- [ ] ch - Cross server chat.
- [ ] nick - Set nickname.
- [x] time - Show server time.
- [x] translate - Enable/disable auto translate
