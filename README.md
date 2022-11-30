# Essentials
![GitHub all releases](https://img.shields.io/github/downloads/kieaer/Essentials/total?style=flat-square)
![GitHub release (by tag)](https://img.shields.io/github/downloads/kieaer/Essentials/v13.4/total?style=flat-square)<br>
Add more commands to the server.<br>
**__Requires Java 16 or higher!__**

This plugin was created because the creators were very angry with too many Griefers in Mindustry 4.0 Alpha version.<br>
To create this plugin, I'm spent all night studying a programming language I hadn't looked at in my entire life. lol<br><br>
Basically, this plugin focuses on finding cheats and griefers, but it adds additional features to make it useful on other servers as well.

## Installation
Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands
| Command   | Parameter                                                                | Description                                                    |
|:----------|:-------------------------------------------------------------------------|:---------------------------------------------------------------|
| vote      | &lt;kick/map/gg/skip/back/random&gt; [player/amount/world_name] [reason] | Start voting                                                   |
| changepw  | &lt;new_password&gt; &lt;password_repeat&gt;                             | Change account password.                                       |
| chars     | &lt;text...&gt;                                                          | Make pixel texts                                               |
| color     |                                                                          | Enable color nickname                                          |
| discord   |                                                                          | Authenticate your Discord account to the server.               |
| effect    | [effect] [x] [y] [rotate] [color]                                        | effects                                                        |
| fillitems | &lt;team&gt;                                                             | Fill the core with items.                                      |
| freeze    | &lt;player&gt;                                                           | Stop player unit movement                                      |
| gg        |                                                                          | Force gameover                                                 |
| god       | [name]                                                                   | Set max player health                                          |
| help      | [page]                                                                   | Show command lists                                             |
| hub       | &lt;zone/block/count/total&gt; [ip] [parameters...]                      | Create a server to server point.                               |
| info      | [player]                                                                 | Show your information                                          |
| js        | [code...]                                                                | Execute JavaScript codes                                       |
| kickall   |                                                                          | All users except yourself and the administrator will be kicked |
| kill      | [player]                                                                 | Kill player.                                                   |
| killall   | [team]                                                                   | Kill all enemy units                                           |
| lang      | &lt;language_tag&gt;                                                     | Set the language for your account.                             |
| login     | &lt;id&gt; &lt;password&gt;                                              | Access your account                                            |
| maps      | [page]                                                                   | Show server maps                                               |
| me        | &lt;text...&gt;                                                          | broadcast * message                                            |
| meme      | &lt;type&gt;                                                             | Enjoy meme features!                                           |
| motd      |                                                                          | Show server motd.                                              |
| mute      | &lt;player&gt;                                                           | Mute player                                                    |
| pause     |                                                                          | Pause server                                                   |
| players   | [page]                                                                   | Show players list                                              |
| ranking   | &lt;time/place/break/attack/exp&gt;                                      | Show players ranking                                           |
| reg       | &lt;id&gt; &lt;password&gt; &lt;password_repeat&gt;                      | Register account                                               |
| report    | &lt;player&gt; &lt;reason...&gt;                                         | Report player                                                  |
| search    | [value]                                                                  | Search player data                                             |
| setperm   | &lt;player&gt; &lt;group&gt;                                             | Set the player's permission group.                             |
| spawn     | &lt;unit/block&gt; &lt;name&gt; [amount/rotate]                          | Spawns units at the player's location.                         |
| status    |                                                                          | Show server status                                             |
| team      | &lt;team_name&gt; [name]                                                 | Change team                                                    |
| tempban   | &lt;player&gt; &lt;time&gt; [reason]                                     | Ban the player for a certain period of time.                   |
| time      |                                                                          | Show server time                                               |
| tp        | &lt;player&gt;                                                           | Teleport to other players                                      |
| tpp       | [player]                                                                 | Lock on camera the target player.                              |
| track     |                                                                          | Displays the mouse positions of players.                       |
| unmute    | &lt;player&gt;                                                           | Unmute player                                                  |
| url       | &lt;command&gt;                                                          | Opens a URL contained in a specific command.                   |
| weather   | &lt;rain/snow/sandstorm/sporestorm&gt; &lt;seconds&gt;                   | Adds a weather effect to the map.                              |


## Server commands
| Command | Parameter                            | Description                                  |
|:--------|:-------------------------------------|:---------------------------------------------|
| debug   | [bool]                               | Show plugin internal informations            |
| gen     |                                      | Generate README.md texts                     |
| reload  |                                      | Reload permission and config files.          |
| setperm | &lt;player&gt; &lt;group&gt;         | Set the player's permission group.           |
| tempban | &lt;player&gt; &lt;time&gt; [reason] | Ban the player for a certain period of time. |

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