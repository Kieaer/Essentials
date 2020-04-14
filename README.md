# Essentials
Add more commands to the server.

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Requirements for running this plugin
Minimum require java version: __Java 11 or higher__<br>
This plugin does a lot of disk read/write operations depending on the features usage.

### Minimum
CPU: Athlon 200GE or Intel i5 2300<br>
RAM: 20MB<br>
Disk: HDD capable of more than 2MB/s random read/write.

### Recommend
CPU: Ryzen 3 2200G or Intel i3 8100<br>
RAM: 50MB<br>
Disk: HDD capable of more than 5MB/s random read/write.

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands

| Command | Parameter | Description |
|:---|:---|:--- |
| help | [page] | Lists all commands. |
| t | &lt;message...&gt; | Send a message only to your teammates. |
| sync |  | Re-synchronize world state. |
| help | [page] | Show command lists |

## Server commands

| Command | Parameter | Description |
|:---|:---|:--- |
| help |  | Displays this command list. |
| version |  | Displays server version info. |
| exit |  | Exit the server application. |
| stop |  | Stop hosting the server. |
| host | [mapname] [mode] | Open the server. Will default to survival and a random map if not specified. |
| maps |  | Display all available maps. |
| reloadmaps |  | Reload all maps from disk. |
| status |  | Display server status. |
| mods |  | Display all loaded mods. |
| mod | &lt;name...&gt; | Display information about a loaded plugin. |
| js | &lt;script...&gt; | Run arbitrary Javascript. |
| say | &lt;message...&gt; | Send a message to all players. |
| difficulty | &lt;difficulty&gt; | Set game difficulty. |
| rules | [remove/add] [name] [value...] | List, remove or add global rules. These will apply regardless of map. |
| fillitems | [team] | Fill the core with items. |
| playerlimit | [off/somenumber] | Set the server player limit. |
| config | [name] [value...] | Configure server settings. |
| subnet-ban | [add/remove] [address] | Ban a subnet. This simply rejects all connections with IPs starting with some string. |
| whitelisted |  | List the entire whitelist. |
| whitelist-add | &lt;ID&gt; | Add a player to the whitelist by ID. |
| whitelist-remove | &lt;ID&gt; | Remove a player to the whitelist by ID. |
| shuffle | [none/all/custom/builtin] | Set map shuffling mode. |
| nextmap | &lt;mapname...&gt; | Set the next map to be played after a game-over. Overrides shuffling. |
| kick | &lt;username...&gt; | Kick a person by name. |
| ban | &lt;type-id/name/ip&gt; &lt;username/IP/ID...&gt; | Ban a person. |
| bans |  | List all banned IPs and IDs. |
| unban | &lt;ip/ID&gt; | Completely unban a person by IP or ID. |
| admin | &lt;add/remove&gt; &lt;username/ID...&gt; | Make an online user admin |
| admins |  | List all admins. |
| runwave |  | Trigger the next wave. |
| load | &lt;slot&gt; | Load a save from a slot. |
| save | &lt;slot&gt; | Save game state to a slot. |
| saves |  | List all saves in the save directory. |
| gameover |  | Force a game over. |
| info | &lt;IP/UUID/name...&gt; | Find player info(s). Can optionally check for all names or IPs a player has had. |
| search | &lt;name...&gt; | Search players who have used part of a name. |
| gc |  | Trigger a grabage struct. Testing only. |
| admin | &lt;name&gt; | Set admin status to player. |
| info | &lt;player/uuid&gt; | Show player information |

README.md Generated time: 2020-04-14 22:49:06