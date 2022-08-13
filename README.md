[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Kieaer_Essentials)<br>
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Kieaer_Essentials)
# Essentials
Add more commands to the server.<br>
**__Requires Java 18 or higher!__**

## Installation
Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands
| Command | Parameter | Description |
|:---|:---|:--- |
| chars | &lt;text...&gt; | Make pixel texts |
| color |  | Enable color nickname |
| killall | [team] | Kill all enemy units |
| help | [page] | Show command lists |
| info |  | Show your information |
| hub | &lt;zone/block/count/total&gt; [ip] [parameters...] | Create a server-to-server warp zone. |
| kill | [player] | Kill player. |
| login | &lt;id&gt; &lt;password&gt; | Access your account |
| maps | [page] | Show server maps |
| me | &lt;text...&gt; | broadcast * message |
| motd |  | Show server motd. |
| players | [page] | Show players list |
| meme | &lt;type&gt; | Router |
| register | &lt;accountid&gt; &lt;password&gt; | Register account |
| spawn | &lt;unit/block&gt; &lt;name&gt; [amount/rotate] | Spawn mob in player position |
| status |  | Show server status |
| team | &lt;team_name&gt; [player_name] | Change team |
| time |  | Show server time |
| tp | &lt;player&gt; | Teleport to other players |
| weather | &lt;rain/snow/sandstorm/sporestorm&gt; &lt;seconds&gt; | Change map light |
| mute | &lt;Player_name&gt; | Mute player |
| unmute | &lt;Player_name&gt; | Unmute player |
| config | &lt;name&gt; [value] | Edit server config |
| gg | [delay] | Force gameover |
| effect | [effects] | effects |
| god | [Player_name] | Set max player health |
| random |  | Random events |
| pause |  | Pause server |
| js | [code] | Execute JavaScript codes |
| search | [value] | Search player data |


## Server commands
| Command | Parameter | Description |
|:---|:---|:--- |
| gen |  | Generate README.md texts |


README.md Generated time: 2022-08-05 13:54:36

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