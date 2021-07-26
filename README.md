[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Kieaer_Essentials) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/dashboard?id=Kieaer_Essentials)
# Essentials
Add more commands to the server.

**__Requires Java 16 or higher!__**

I'm getting a lot of suggestions.<br>
Please submit your idea to this repository issues or Mindustry official discord!

## Next plans
- [ ] Fix bugs
  - [ ] Voting not working
- [ ] Fix many typos
- [ ] Features separation
  - [ ] Web server
    - [ ] Fix a sometimes ranking site not loaded
- [ ] New features
  - [ ] Web console
    - [ ] Control plugin database
    - [ ] Check world status
      - [ ] Dynmap (idea from Minecraft)
      - [ ] Rest API
- [ ] All code clean

## Installation

Put this plugin in the ``<server folder location>/config/mods`` folder.

## Client commands
| Command | Parameter | Description |
|:---|:---|:--- |
| ch |  | Send chat to another server. |
| changepw | &lt;new_password&gt; &lt;new_password_repeat&gt; | Change account password |
| chars | &lt;Text...&gt; | Make pixel texts |
| color |  | Enable color nickname |
| killall |  | Kill all enemy units |
| help | [page] | Show command lists |
| info |  | Show your information |
| warp | &lt;zone/block/count/total&gt; [ip] [parameters...] | Create a server-to-server warp zone. |
| kill | [player] | Kill player. |
| login | &lt;id&gt; &lt;password&gt; | Access your account |
| maps | [page] | Show server maps |
| me | &lt;text...&gt; | broadcast * message |
| motd |  | Show server motd. |
| players | [page] | Show players list |
| save |  | Auto rollback map early save |
| router |  | Router |
| register |  | Register account |
| spawn | &lt;unit/block&gt; &lt;name&gt; [amount/rotate] | Spawn mob in player position |
| status |  | Show server status |
| team | &lt;team_name&gt; | Change team |
| time |  | Show server time |
| tp | &lt;player&gt; | Teleport to other players |
| weather | &lt;rain/snow/sandstorm/sporestorm&gt; &lt;seconds&gt; | Change map light |
| mute | &lt;Player_name&gt; | Mute/unmute player |

## Server commands
| Command | Parameter | Description |
|:---|:---|:--- |
| gendocs |  | Generate Essentials README.md |
| lobby |  | Toggle lobby server features |
| saveall |  | Manually save all plugin data |
| bansync |  | Synchronize ban list with server |
| info | &lt;player/uuid&gt; | Show player information |
| reload |  | Reload Essential plugin data |
| blacklist |  | <add/remove> [name] |
| debug | &lt;code&gt; | Debug message. plugin developer only. |

README.md Generated time: 2021-07-26 19:53:49