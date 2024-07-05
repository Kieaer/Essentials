# Essentials
![GitHub all releases](https://img.shields.io/github/downloads/kieaer/Essentials/total?style=flat-square)
![GitHub release (latest by date)](https://img.shields.io/github/downloads/kieaer/essentials/latest/total?style=flat-square)<br>
Add more commands to the server.<br>
**__Requires Java 17 or higher!__**

This plugin was created because the creators were very angry with too many Griefers in Mindustry 4.0 Alpha version.<br>
To create this plugin, I'm spent all night studying a programming language I hadn't looked at in my entire life. lol<br><br>
Basically, this plugin focuses on finding cheats and griefers, but it adds additional features to make it useful on other servers as well.

## Installation
Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands
| Command    | Parameter                                                           | Description                                                                      |
|:-----------|:--------------------------------------------------------------------|:---------------------------------------------------------------------------------|
| changename | &lt;target&gt; &lt;new_name&gt;                                     | Change player name                                                               |
| changepw   | &lt;new_password&gt; &lt;password_repeat&gt;                        | Change account password.                                                         |
| changemap  | &lt;name&gt; [gamemode]                                             | Change the world or gamemode immediately.                                        |
| chars      | &lt;text...&gt;                                                     | Make pixel texts on ground.                                                      |
| chat       | &lt;on/off&gt;                                                      | Mute all players without admins                                                  |
| color      |                                                                     | Enable color nickname                                                            |
| dps        |                                                                     | Create damage per seconds meter block                                            |
| effect     | &lt;on/off/level&gt; [color]                                        | Turn other players' effects on or off, or set effects and colors for each level. |
| exp        | &lt;set/hide/add/remove&gt; [values/player] [player]                | Edit account exp values                                                          |
| fillitems  | [team]                                                              | Fill the core with items.                                                        |
| freeze     | &lt;player&gt;                                                      | Stop player unit movement                                                        |
| gg         | [team]                                                              | Make game over immediately.                                                      |
| god        | [player]                                                            | Set max player health                                                            |
| help       | [page]                                                              | Show command lists                                                               |
| hub        | &lt;parameter&gt; [ip] [parameters...]                              | Create a server to server point.                                                 |
| hud        | &lt;health/apm&gt;                                                  | Enable information on screen                                                     |
| info       | [player...]                                                         | Show player info                                                                 |
| js         | [code...]                                                           | Execute JavaScript code                                                          |
| kickall    |                                                                     | Kick all players without you.                                                    |
| kill       | [player]                                                            | Kill player's unit.                                                              |
| killall    | [team]                                                              | Kill all enemy units                                                             |
| killunit   | &lt;name&gt; [amount] [team]                                        | Destroy specific units                                                           |
| lang       | &lt;language_tag&gt;                                                | Set the language for current account                                             |
| log        |                                                                     | Enable block history view mode                                                   |
| maps       | [page]                                                              | Show server map lists                                                            |
| meme       | &lt;type&gt;                                                        | Enjoy mindustry meme features!                                                   |
| motd       |                                                                     | Show server's message of the day                                                 |
| mute       | &lt;player&gt;                                                      | Mute player                                                                      |
| pause      |                                                                     | Pause or Unpause map                                                             |
| players    | [page]                                                              | Show current players list                                                        |
| ranking    | &lt;time/exp/attack/place/break/pvp&gt; [page]                      | Show player ranking                                                              |
| rollback   | &lt;player&gt;                                                      | Undo all actions taken by the player.                                            |
| setitem    | &lt;item&gt; &lt;amount&gt; [team]                                  | Set item to team core                                                            |
| setperm    | &lt;player&gt; &lt;group&gt;                                        | Set the player's permission group.                                               |
| skip       | &lt;wave&gt;                                                        | Start n wave immediately                                                         |
| spawn      | &lt;unit/block&gt; &lt;name&gt; [amount/rotate]                     | Spawn units or block at the player's current location.                           |
| status     |                                                                     | Show current server status                                                       |
| strict     | &lt;player&gt;                                                      | Set whether the target player can build or not.                                  |
| t          | &lt;message...&gt;                                                  | Send a meaage only to your teammates.                                            |
| team       | &lt;team&gt; [name]                                                 | Set player team                                                                  |
| time       |                                                                     | Show current server time                                                         |
| tp         | &lt;player&gt;                                                      | Teleport to other players                                                        |
| tpp        | [player]                                                            | Lock on camera the target player                                                 |
| track      |                                                                     | Display the mouse positions of players.                                          |
| unban      | &lt;player&gt;                                                      | Unban player                                                                     |
| unmute     | &lt;player&gt;                                                      | Unmute player                                                                    |
| url        | &lt;command&gt;                                                     | Opens a URL contained in a specific command.                                     |
| vote       | &lt;kick/map/gg/skip/back/random&gt; [player/amount/world] [reason] | Start voting                                                                     |
| votekick   | &lt;player&gt;                                                      | Start kick voting                                                                |
| weather    | &lt;weather&gt; &lt;seconds&gt;                                     | Adds a weather effect to the map.                                                |


## Server commands
| Command  | Parameter                            | Description                                     |
|:---------|:-------------------------------------|:------------------------------------------------|
| chat     | &lt;on/off&gt;                       | Mute all players without admins                 |
| freeze   | &lt;player&gt;                       | Stop player unit movement                       |
| gen      |                                      | Generate wiki docs                              |
| kickall  |                                      | Kick all players.                               |
| kill     | &lt;player&gt;                       | Kill player's unit                              |
| killall  | [team]                               | Kill all units                                  |
| killunit | &lt;name&gt; [amount] [team]         | Destroy specific units                          |
| mute     | &lt;player&gt;                       | Mute player                                     |
| reload   |                                      | Reload essential plugin configs.                |
| setperm  | &lt;player&gt; &lt;group&gt;         | Set the player's permission group.              |
| strict   | &lt;player&gt;                       | Set whether the target player can build or not. |
| team     | &lt;team&gt; &lt;name&gt;            | Set player team                                 |
| tempban  | &lt;player&gt; &lt;time&gt; [reason] | Ban the player for aa certain peroid of time    |
| unload   |                                      | unload essential plugin (experimental)          |
| unmute   | &lt;player&gt;                       | Unmute player                                   |

README.md Generated time: 2024-07-05 13:32:46

## Contribute guide
### File description
* Main - Execute various settings and services to start the plugin.
* Bundle - It's used to add various languages to the plugin.
* Commands - Add server/client commands.
* Config - Loads or saves settings used by the plugin.
* DB - Save and load using SQLite to store player data.
* Event - It works when an event occurs within the game.
* FileWatchService - A service that monitors permissions and config files so that they are applied in real time when you modify them.
* Permission - Read the permission file and check the permissions between players.
* PluginData - Save and load using SQLite to store plugin data.
* Trigger - Save and load player data, start ban sharing server/client, added feature triggers.

## License
**Free License**

You can copy and modify my source code and claim it as your own!<br>
Because if you want to implement a specific features, the source code will be the same after all lol