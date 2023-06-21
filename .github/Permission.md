# What's permission in this plugin?
The permission feature in this plugin is designed to set available commands for each user or group.<br>
It is the same admin, but you do not want to use some command, or you are a general user, but you use it to set special command privileges.

Wiki version: Essentials 18

# How to use?

In the permission_user.txt file, you can modify it as below.

```yaml
{
  Group name:
  {
    admin: true or false. It will add admin mark in players list.
    chatFormat: %1 is player name, %2 is message. It must be written inside " ". Example "%1[orange] > [white]%2"
    inheritance: write inheritance group if you want.
    default: true or false. You can set only one default group.
    permission:
    [
      Write permission nodes here.
    ]
  }
}
```

# Avaliable permission nodes
| Command   | Permission node | Description                                                                  | Default group |
|:----------|:----------------|:-----------------------------------------------------------------------------|:--------------|
|           | afk.admin       | Groups with this privilege will not be kicked even if they do nothing.       | admin         |
|           | all             | Allows access to the **ALL** commands.                                       | owner         |
|           | chat.admin      | Allow Chat is possible when all players are muted from chatting              | admin         |
|           | exp.admin       | Allows permission to edit experience points.                                 | admin         |
|           | hub.build       | Allows users to build on servers with hub mode enabled.                      |               |
|           | kick.admin      | The user will have the permission not to be kick with the ``/vote`` command. | admin         |
|           | pvp.spector     | Player will automatically enter spectator mode on PvP server.                | admin         |
| broadcast | broadcast       | Allows access to the ``/broadcast`` command.                                 |               |
| changemap | changemap       | Allows access to the ``/changemap`` command.                                 | admin         |
| changepw  | changepw        | Allows access to the ``/changepw`` command.                                  | user          |
| chars     | chars           | Allows access to the ``/chars`` command.                                     | admin         |
| color     | color           | Allows access to the ``/color`` command.                                     | admin         |
| discord   | discord         | Allows access to the ``/discord`` command.                                   | user          |
| dps       | dps             | Allows access to the ``/dps`` command.                                       | admin         |
| effect    | effect          | Allows access to the ``/effect`` command.                                    | user          |
| exp       | exp             | Allows access to the ``/exp`` command.                                       | user          |
| fillitems | fillitems       | Allows access to the ``/fillitems`` command.                                 | admin         |
| freeze    | freeze          | Allows access to the ``/freeze`` command.                                    | admin         |
| gg        | gg              | Allows access to the ``/gg`` command.                                        | admin         |
| god       | god             | Allows access to the ``/god`` command.                                       | admin         |
| help      | help            | Allows access to the ``/help`` command.                                      | visitor       |
| hub       | hub             | Allows access to the ``/hub`` command.                                       |               |
| hud       | hud             | Allows access to the ``/hud`` command.                                       | user          |
| hud       | hud.enemy       | Allows users to see the health of enemy units.                               | admin         |
| info      | info            | Allows access to the ``/info`` command.                                      | user          |
| info      | info.other      | Allows permission to view other players info using the ``/info`` command.    | admin         |
| js        | js              | Allows access to the ``/js`` command.                                        |               |
| kickall   | kickall         | Allows access to the ``/kickall`` command.                                   |               |
| kill      | kill            | Allows access to the ``/kill`` command.                                      | admin         |
| kill      | kill.other      | Allow permission to kill another player's unit.                              | admin         |
| killall   | killall         | Allows access to the ``/killall`` command.                                   |               |
| killunit  | killunit        | Allows access to the ``/killunit`` command.                                  |               |
| lang      | lang            | Allows access to the ``/lang`` command.                                      | user          |
| login     | login           | Allows access to the ``/login`` command.                                     | visitor       |
| maps      | maps            | Allows access to the ``/maps`` command.                                      | user          |
| me        | me              | Allows access to the ``/me`` command.                                        | user          |
| meme      | meme            | Allows access to the ``/meme`` command.                                      | admin         |
| motd      | motd            | Allows access to the ``/motd`` command.                                      | user          |
| mute      | mute            | Allows access to the ``/mute`` command.                                      | admin         |
| pause     | pause           | Allows access to the ``/pause`` command.                                     | admin         |
| players   | players         | Allows access to the ``/players`` command.                                   | user          |
| pm        | pm              | Allows access to the ``/pm`` command.                                        | user          |
| pm        | pm.other        | Allows access to view other players' private messages                        | admin         |
| ranking   | ranking         | Allows access to the ``/ranking`` command.                                   | user          |
| reg       | reg             | Allows access to the ``/reg`` command.                                       | visitor       |
| report    | report          | Allows access to the ``/report`` command.                                    | user          |
| rollback  | rollback        | Allows access to the ``/rollback`` command.                                  | admin         |
| search    | search          | Allows access to the ``/search`` command.                                    | admin         |
| setperm   | setperm         | Allows access to the ``/setperm`` command.                                   |               |
| spawn     | spawn           | Allows access to the ``/spawn`` command.                                     | admin         |
| status    | status          | Allows access to the ``/status`` command.                                    | user          |
| team      | team            | Allows access to the ``/team`` command.                                      | admin         |
| team      | team.other      | Allow permission to change another player's team.                            | admin         |
| tempban   | tempban         | Allows access to the ``/tempban`` command.                                   | admin         |
| time      | time            | Allows access to the ``/time`` command.                                      | user          |
| tp        | tp              | Allows access to the ``/tp`` command.                                        | user          |
| tpp       | tpp             | Allows access to the ``/tpp`` command.                                       | admin         |
| track     | track           | Allows access to the ``/track`` command.                                     | user          |
| unmute    | unmute          | Allows access to the ``/unmute`` command.                                    | admin         |
| url       | url             | Allows access to the ``/url`` command.                                       | user          |
| vote      | vote            | Allows access to the ``/vote`` command.                                      | user          |
| vote      | vote.back       | Allows access to the ``/vote back`` command.                                 | user          |
| vote      | vote.gg         | Allows access to the ``/vote gg`` command.                                   | user          |
| vote      | vote.kick       | Allows access to the ``/vote kick`` command.                                 | user          |
| vote      | vote.map        | Allows access to the ``/vote map`` command.                                  | user          |
| vote      | vote.pass       | The user can immediately pass or reject the vote                             | admin         |
| vote      | vote.random     | Allows access to the ``/vote random`` command.                               | user          |
| vote      | vote.skip       | Allows access to the ``/vote skip`` command.                                 | user          |
| weather   | weather         | Allows access to the ``/weather`` command.                                   | admin         |

