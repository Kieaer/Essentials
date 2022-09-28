[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Kieaer_Essentials)<br>
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Kieaer_Essentials)
# Essentials
Add more commands to the server.<br>
**__Requires Java 16 or higher!__**

## Installation
Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands
| Command   | Parameter                                                                | Description                                                    |
|:----------|:-------------------------------------------------------------------------|:---------------------------------------------------------------|
| chars     | &lt;text...&gt;                                                          | Make pixel texts                                               |
| color     |                                                                          | Enable color nickname                                          |
| discord   |                                                                          | Authenticate your Discord account to the server.               |
| effect    | [effect] [x] [y] [rotate] [color]                                        | effects                                                        |
| fillitems | &lt;team&gt;                                                             | Fill the core with items.                                      |
| gg        |                                                                          | Force gameover                                                 |
| god       | [name]                                                                   | Set max player health                                          |
| help      | [page]                                                                   | Show command lists                                             |
| hub       | &lt;zone/block/count/total&gt; [ip] [parameters...]                      | Create a server to server point.                               |
| info      | [player]                                                                 | Show your information                                          |
| js        | [code]                                                                   | Execute JavaScript codes                                       |
| kickall   |                                                                          | All users except yourself and the administrator will be kicked |
| kill      | [player]                                                                 | Kill player.                                                   |
| killall   | [team]                                                                   | Kill all enemy units                                           |
| login     | &lt;id&gt; &lt;password&gt;                                              | Access your account                                            |
| maps      | [page]                                                                   | Show server maps                                               |
| me        | &lt;text...&gt;                                                          | broadcast * message                                            |
| meme      | &lt;type&gt;                                                             | Enjoy meme features!                                           |
| motd      |                                                                          | Show server motd.                                              |
| mute      | &lt;player&gt;                                                           | Mute player                                                    |
| pause     |                                                                          | Pause server                                                   |
| players   | [page]                                                                   | Show players list                                              |
| reg       | &lt;id&gt; &lt;password&gt; &lt;password_repeat&gt;                      | Register account                                               |
| search    | [value]                                                                  | Search player data                                             |
| setperm   | &lt;player&gt; &lt;group&gt;                                             | Set the player's permission group.                             |
| spawn     | &lt;unit/block&gt; &lt;name&gt; [amount/rotate]                          | Spawns units at the player's location.                         |
| status    |                                                                          | Show server status                                             |
| team      | &lt;team_name&gt; [name]                                                 | Change team                                                    |
| tempban   | &lt;player&gt; &lt;time&gt; [reason]                                     | Ban the player for a certain period of time.                   |
| time      |                                                                          | Show server time                                               |
| tp        | &lt;player&gt;                                                           | Teleport to other players                                      |
| unmute    | &lt;player&gt;                                                           | Unmute player                                                  |
| url       | &lt;command&gt;                                                          | Opens a URL contained in a specific command.                   |
| vote      | &lt;kick/map/gg/skip/back/random&gt; [player/amount/world_name] [reason] | Start voting                                                   |
| weather   | &lt;rain/snow/sandstorm/sporestorm&gt; &lt;seconds&gt;                   | Adds a weather effect to the map.                              |

## Server commands
| Command | Parameter                            | Description                                  |
|:--------|:-------------------------------------|:---------------------------------------------|
| debug   | [bool]                               | Show plugin internal informations            |
| gen     |                                      | Generate README.md texts                     |
| setperm | &lt;player&gt; &lt;group&gt;         | Set the player's permission group.           |
| tempban | &lt;player&gt; &lt;time&gt; [reason] | Ban the player for a certain period of time. |

README.md Generated time: 2022-08-27 19:04:45

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