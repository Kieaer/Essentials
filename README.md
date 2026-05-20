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
| Command      | Parameter                                                               | Description                                                                      |
|:-------------|:------------------------------------------------------------------------|:---------------------------------------------------------------------------------|
| achievements | [page]                                                                  | Show your achievements                                                           |
| broadcast    | &lt;message...&gt;                                                      | Send message to all connected servers                                            |
| changemap    | &lt;name&gt; [gamemode]                                                 | Change the world or game mode immediately.                                       |
| changename   | &lt;target&gt; &lt;new_name&gt;                                         | Change player name                                                               |
| changepw     | &lt;new_password&gt; &lt;password_repeat&gt;                            | Change account password.                                                         |
| chars        | &lt;text...&gt;                                                         | Make pixel texts on ground.                                                      |
| chat         | &lt;on/off&gt;                                                          | Mute all players without admins                                                  |
| color        |                                                                         | Enable color nickname                                                            |
| discord      |                                                                         | Open server discord url                                                          |
| effect       | &lt;on/off/level&gt; [color]                                            | Turn other players' effects on or off, or set effects and colors for each level. |
| exp          | &lt;set/hide/add/remove&gt; [values/player] [player]                    | Edit account exp values                                                          |
| fillitems    | [team]                                                                  | Fill the core with items.                                                        |
| fuck         | [command]                                                               | Corrects and executes a command with typos                                       |
| gg           | [team]                                                                  | Make game over immediately.                                                      |
| god          | [player]                                                                | Set max player health                                                            |
| help         | [page]                                                                  | Show command lists                                                               |
| hub          | &lt;parameter&gt; [ip] [parameters...]                                  | Create a server to server point.                                                 |
| info         | [player...]                                                             | Show player info                                                                 |
| js           | [code...]                                                               | Execute JavaScript code                                                          |
| kickall      |                                                                         | Kick all players without admins.                                                 |
| kill         | [player]                                                                | Kill player's unit.                                                              |
| killall      | [team]                                                                  | Kill all enemy units                                                             |
| killunit     | &lt;name&gt; [amount] [team]                                            | Destroy specific units                                                           |
| login        | &lt;id&gt; &lt;password&gt;                                             | Log-in to account.                                                               |
| log          |                                                                         | Enable block history view mode                                                   |
| maps         | [page]                                                                  | Show server map lists                                                            |
| me           | &lt;text...&gt;                                                         | Chat with special prefix                                                         |
| meme         | &lt;type&gt;                                                            | Enjoy mindustry meme features!                                                   |
| motd         |                                                                         | Show server's message of the day                                                 |
| mute         | &lt;player&gt;                                                          | Mute player                                                                      |
| nextmap      | [map]                                                                   | Set the next map to move to after game over                                      |
| pause        |                                                                         | Pause or Unpause map                                                             |
| players      | [page]                                                                  | Show current players list                                                        |
| pm           | &lt;player&gt; &lt;message...&gt;                                       | Send a private message                                                           |
| ranking      | &lt;time/exp/attack/place/break/pvp&gt; [page]                          | Show player ranking                                                              |
| reg          | &lt;id&gt; &lt;password&gt; &lt;password_repeat&gt;                     | Register account                                                                 |
| report       | &lt;player&gt; &lt;reason...&gt;                                        | Report a player                                                                  |
| rollback     | &lt;player&gt;                                                          | Undo all actions taken by the player.                                            |
| setitem      | &lt;item&gt; &lt;amount&gt; [team]                                      | Set item to team core                                                            |
| setperm      | &lt;player&gt; &lt;group&gt;                                            | Set the player's permission group.                                               |
| skip         | &lt;wave&gt;                                                            | Start n wave immediately                                                         |
| spawn        | &lt;unit/block&gt; &lt;name&gt; [amount(rotate)/block_team] [unit_team] | Spawn units or block at the player's current location.                           |
| status       |                                                                         | Show current server status                                                       |
| strict       | &lt;player&gt;                                                          | Set whether the target player can build or not.                                  |
| t            | &lt;message...&gt;                                                      | Send a meaage only to your teammates.                                            |
| team         | &lt;team&gt; [name]                                                     | Set player team                                                                  |
| time         |                                                                         | Show current server time                                                         |
| tp           | &lt;player&gt;                                                          | Teleport to other players                                                        |
| track        |                                                                         | Display the mouse positions of players.                                          |
| unban        | &lt;player&gt;                                                          | Unban player                                                                     |
| unmute       | &lt;player&gt;                                                          | Unmute player                                                                    |
| url          | &lt;command&gt;                                                         | Opens a URL contained in a specific command.                                     |
| vote         | &lt;kick/map/gg/skip/back/random&gt; [player/amount/world] [reason]     | Start voting                                                                     |
| votekick     | &lt;player&gt;                                                          | Start kick voting                                                                |
| weather      | &lt;weather&gt; &lt;seconds&gt;                                         | Adds a weather effect to the map.                                                |
| ws           | [args...]                                                               | WorldEdit selection and block manipulation                                       |


## Server commands
| Command          | Parameter                            | Description                                                         |
|:-----------------|:-------------------------------------|:--------------------------------------------------------------------|
| chat             | &lt;on/off&gt;                       | Mute all players without admins                                     |
| debug            | [parameter...]                       | Debug any commands                                                  |
| gen              |                                      | Generate wiki docs                                                  |
| kickall          |                                      | Kick all players.                                                   |
| kill             | &lt;player&gt;                       | Kill player's unit                                                  |
| killall          | [team]                               | Kill all units                                                      |
| killunit         | &lt;name&gt; [amount] [team]         | Destroy specific units                                              |
| mergeplayer      | &lt;from_uuid&gt; &lt;to_uuid&gt;    | Merge two player accounts (from &rarr; to).                         |
| mute             | &lt;player&gt;                       | Mute player                                                         |
| reload           |                                      | Reload essential plugin configs.                                    |
| setfeedbackprovider | &lt;player&gt;                   | Set the FeedbackProvider achievement for a player                   |
| setmapprovider   | &lt;player&gt;                       | Set the MapProvider achievement for a player                        |
| setperm          | &lt;player&gt; &lt;group&gt;         | Set the player's permission group.                                  |
| strict           | &lt;player&gt;                       | Set whether the target player can build or not.                     |
| team             | &lt;team&gt; &lt;name&gt;            | Set player team                                                     |
| tempban          | &lt;player&gt; &lt;time&gt; [reason] | Ban the player for a certain period of time                         |
| unmute           | &lt;player&gt;                       | Unmute player                                                       |

README.md Generated time: 2026-05-20 20:44

## License
**Free License**

You can copy and modify my source code and claim it as your own!<br>
Because if you want to implement a specific features, the source code will be the same after all lol