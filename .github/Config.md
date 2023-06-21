Wiki version: Essentials 18

The plugin already has a description, but you can check it in more detail here.<br>
Plugin config file path: ``config/mods/Essentials/config.txt``<br>
Search for the setting you are looking for using the ``Ctrl+F`` keys.

# Plugin

Manage the plugin's preferences.

## ⚙️ update

Default: ``true``<br>
Check for plugin updates every time you start the server.<br>
However, it does not download automatically.

Example
```yaml
# Check for plugin update
update: true

# Disable check for plugin update
update: false
```

## ⚙️ report

Default: ``true``<br>
In case of an error, it sends error information to the developer's server.<br>
However, the current developer's server does not have an error reporting server, so the feature does not work.

Example
```yaml
# Enable upload error reports
report: true

# Disable upload error report
report: false
```

## ⚙️ authType

Default: ``None``<br>
Avaliable: none, password, discord<br>
If set to ``None``, users will be able to play without further authentication.<br>
If set to ``Password``, users must register for an account using the ``/reg`` command to play.<br>
If set to ``Discord``, users must authenticate with Discord to play on the server.

It is rare to use anything higher than the normal Password level.<br>
However, in the event of a high modified client threat or if the UUID and IP values given by the client are no longer reliable, both UUID and IP values are ignored and the user is identified only with the password-protected account.<br>
Discord is more secure than Password.<br>
The server barrier to entry can be very high, so be careful.

Example
```yaml
# Disable all authentication
# Players can play the server directly without doing anything specific
authType: none

# Use password authentication
# Players must register their account using the /register command.
authType: password

# Use discord authentication
# Players must verify their Discord account on the Discord server using the /discord command before they can play on the server.
authType: discord
```

## ⚙️ database

Default: ``Your server folder/config/mods/Essentials/database``<br>
Select the DB where the player data will be saved.<br>
If you enter an IP address here, it will try to connect to the DB with that IP.

Entering ``default`` reverts to the original value.

Example
```yaml
# Windows jOS
database: E:/Mindustry/config/mods/Essentials/database

# Linux OS
database: /home/cloud/mindustry/config/mods/Essentials/database

# If you want to use default values
database: default

# Connect remote database (Servers using this plugin must be turned on.)
database: 192.168.0.2
```

## ⚙️ banList

Default: ``Your server folder/config/mods/Essentials/data``<br>
Set the folder path to manage blocking by inserting or removing the UUID or IP to block into a text file.<br>
UUID is recorded in ``idban.txt`` and IP is recorded in ``ipban.txt``.<br>

Example
```yaml
# Windows OS
database: E:/Mindustry/config/mods/Essentials/data

# Linux OS
database: /home/cloud/mindustry/config/mods/Essentials/data

# Use other ban list folder
database: E:/Mindustry_sandbox/config/mods/Essentials/data
```

# Features

Manage the features of plugins.

## 💤 afk

Default: ``false``<br>
If the player doesn't do anything for a certain amount of time, they are automatically kicked.

Example
```yaml
# Enable auto kick
afk: true

# Disable auto kick
afk: false
```

## 💤 afkTime
Default: ``300`` (5 minute)<br>
Set how many seconds to kick. The unit is 1 second.

Example
```yaml
# Kick after 5 minutes
afkTime: 300

# Kick after 60 seconds
afkTime: 60

# Kick after 10 minute
afkTime: 600
```

## 💤 afkServer

Default: empty<br>
If this value is non-empty, players who haven't done anything for a certain amount of time will be moved to the server entered here instead of kicking.

Example
```yaml
# If you want to only kick from server
afkServer: ""

# If you want to move to another server instead of kick when it is time for afk
afkServer: mindustry.kr

# Also you can write port...
afkServer: mindustry.kr:6567
```

## ❎ border

Default: ``false``<br>
When out of the world, the unit is destroyed.<br>
On PvP servers, you can prevent units from attacking by diverting them out of the world.

Example
```yaml
# Enable world border
border: true

# Disable world border
border: false
```

## 💬 chatFormat

Default: ``%1[orange] >[white] %2``<br>
Change chat default format.<br>
%1 is player name, %2 is message.

Example
```yaml
# Result is "Gureumi > hello!"
chatFormat: "%1[orange] >[white] %2"

# Result is "[Owner]Gureumi > hello!"
chatFormat: "[sky][Owner]%1[orange] >[white] %2"

# Result is "prefix Gureumi -> hello! suffix"
chatFormat: "prefix %1 -> %2 suffix"
```

## 🤖 spawnLimit

Default: ``3000``<br>
Built to avoid server load.<br>
Specifies the maximum number of units the server will accept.

If you set max units to 0, you won't be able to spawn any units, including players.

Example
```yaml
# If you want to limit the maximum number of units in the world to 3000 units
spawnLimit: 3000

# If you want to 500 units..
spawnLimit: 500
```

## 🗳️ vote

Default: true<br>
Set whether to use the voting function provided by the plugin.<br>
If set to ``true`` , you can use kick, gameover, wave skip, random event, etc.<br>
If set to ``false`` , the default voting functionality provided by the developer will be used.

Example
```yaml
# Enable plugin vote system
vote: true

# Disable plugin vote system and use mindustry native voting system
vote: false
```

## 🔒 fixedName

Default: ``true``<br>
It is fixed to the name that the player used when he first entered the server.<br>
After that, even when the player changes his name and enters, it will be changed to the name he used when he first entered.
This feature is useful for find griefer by changing the nickname or identifying a specific user.

Example
```yaml
# Prevent players from changing their names.
fixedName: true

# Allow players to change their name at will and enter the server.
fixedName: false
```

## 🔒 antiVPN

Default: false<br>
When a player connects to the IP used by the VPN service, the connection is automatically disconnected.

This feature may not work perfectly due to the nature of the VPN service.

Example
```yaml
# Enable antiVPN service
antiVPN: true

# Disable antiVPN service
antiVPN: false
```

## ⚔️ pvpPeace

Default: ``false``<br>
When the world starts on a PvP server, the attack damage of units and buildings is set to 0% for a certain period of time.

Example
```yaml
# Sets the damage to 0% for the time set in pvpPeaceTime.
pvpPeace: true

# Remove PvP restrictions.
pvpPeace: false
```

## ⚔️ pvpPeaceTime

Default: ``300`` (5 minute)<br>
Sets the time to restore attack damage back to normal. The unit is 1 second.

Example
```yaml
# Set 5 minute
pvpPeace: 300

# Set 10 minute
pvpPeace: 600

# Set 30 seconds
pvpPeace: 30
```

## ⚔️ pvpSpector

Default: ``false``<br>
When using this feature, the losing team will automatically move to the Derelict team, and will remain in spectator mode until the next match.

Example
```yaml
# Enable spector system
pvpSpector: true

# Disable spector system
pvpSpector: false
```

## ⏱️ rollbackTime

Default: ``10`` (10 minute)<br>
Saves the state to be used in /vote back at each set time.<br>
If the time is too short, the griefing will not be fully recoverable, and if the time is too long, a significant amount of play data may be lost.<br>
I'm recommand set to 10 minute in attack/survival mode.

```yaml
# Set 5 minute
rollbackTime: 300

# Set 10 minute
rollbackTime: 600

# Set 30 seconds
rollbackTime: 30
```

## 🔊 message

Default: ``false``<br>
Set whether or not to output messages at regular intervals.<br>
You can use this feature to periodically deliver news and information to players.

Example
```yaml
# Enable motd message
message: true

# Disable motd message
message: false
```

## 🔊 messageTime

Default: ``10`` (10 minute)<br>
Set the time interval to output messages.<br>
If the time is too short, the chat window may be spammed.

Example
```yaml
# Set 10 minute
messageTime: 10

# Set 30 minute
messageTime: 30

# Set 5 minute
messageTime: 5
```

## 🔢 countAllServers

Default: ``false``<br>
This is a useful feature on lobby servers.<br>
Adds the number of players shown using the ``/warp count`` command to display the current number of players.

Example
```yaml
# Enable count all connected server players
countAllServers: true

# Only count current server players
countAllServers: false
```

## 🧱 destroyCore

Default: ``false``<br>
When the player team capture the enemy core, to prevent the core from being captured again because the surrounding buildings are not destroyed, all the surrounding buildings are destroyed when the core is captured.<br>
Useful on attack servers.

Example
```yaml
# Enable this feature
destroyCore: true

# Disable this feature
destroyCore: false
```

## 💬 chatlimit

Default: ``false``<br>
Restrict chatting in languages other than a specific language.

This feature may not be perfect.

Example
```yaml
# Block chats outside of specific languages.
chatlimit: true

# Allow all language chats.
chatlimit: false
```

## 💬 chatlanguage

Default: ``ko,en`` (Korean and English. Plugin developer from South Korea.)<br>
Avaliable languages: en, ja, ko, ru, uk, zh<br>
The plugin size has become too large to include all languages...

Set the languages to be allowed.<br>
This plugin uses ISO 3166-1 alpha-2.<br>
Check out the list of languages for country here.<br>
[https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2)

Example
```yaml
# Only use Korean
chatlanguage: ko

# Only use English
chatlanguage: en

# Use Korean, English, Japanese
chatlanguage: ko,en,jp

# Use Japanese, Korean, English, Chinese, Russian
chatlanguage: jp,ko,en,zh,ru
```

## 💬 chatBlacklist

Default: ``false``<br>
Disable certain words.<br>
You can set the languages to be banned in ``chat_blastlist.txt``<br>
Applied one per line.

Example
```yaml
# Enable this feature
chatBlacklist: true

# Disable this feature
chatBlacklist: false
```

## 💬 chatBlacklistRegex

Default: false<br>
The contents written in ``chat_blacklist.txt`` are used as regular expressions, not simple word matching.

Example
```yaml
# Use regular expressions.
chatBlacklistRegex: true

# Just simply searches for word matches.
chatBlacklistRegex: true
```

## ⏫ expDisplay

Default: ``false``<br>
On every player's screen, display players account experience.

Example
```yaml
# Enable this feature
expDisplay: true

# Disable this feature
expDisplay: false
```

## 🔒 blockIP

Default: ``false``<br>
This feature is supported only in **Linux**, and requires the ``iptables`` command in the system to work.<br>
This feature hides this server from the banned user's list of community servers.

Example
```yaml
# Activate the feature to turn on special blocking.
blockIP: true

# Use normal ban
blockIP: false
```

## 💀 waveskip

Default: ``1``<br>
In Survival mode, each wave starts as many times as the set number of waves.

Setting a value that is too high can cause a very high load on the server or cause high gameovers.

Example
```yaml
# Normal wave processed
waveskip: 1

# 2 waves are processed for each wave.
waveskip: 2

# 3 waves are processed for each wave.
waveskip: 3

# 10 waves are processed for each wave.
waveskip: 10
```

## 🧱 unbreakableCore

Default: ``false``<br>
Useful for sandbox servers.<br>
Keep set core health to 100% to avoid destroying the core.

If you do it in wave, attack server the game may not end forever.

Example
```yaml
# Makes unbreakable core
unbreakableCore: true

# Enable destroyable core
unbreakableCore: false
```

## 🌠 moveEffects

Default: ``true``<br>
Enables/disables effects that appear when players move.<br>
Enabling this feature will have different effects for different player levels, but will affect CPU performance and network traffic.

Turns all player movement effects on and off independently of the player's settings.

Example
```yaml
# Enable unit move effects
moveEffects: true

# Disable unit move effects
moveEffects: false
```

## 🖥️ webServer

Default: ``false``<br>
Enable web server. It includes a leaderboard feature.<br>
You may need to use nginx port forwarding to use it as a real domain.

Example
```yaml
# Enable leaderboard web server
webServer: true

# Disable web server
webServer: false
```

## 🖥️ webServerPort

Default: ``8123``<br>
Set the port to use for the web server.

Example
```yaml
# Set web server port to 80
webServerPort: 80

# Set web server port to 8080
webServerPort: 8080
```

## 🖥️ restAPIRequestsLimit

Default: ``5``<br>
The number of times to request per ``restAPILimitRefillPeriod`` config value.<br>
It prevents malicious users from performing attacks that slow down the server by continuously refreshing through the web server.

Example
```yaml
# If restAPILimitRefillPeriod value is 30..
# Set restAPILimitRefillPeriod x 5 = Limit request 5 times per 30 seconds
restAPIRequestsLimit: 5

# Set restAPILimitRefillPeriod x 10 = Limit request 10 times per 30 seconds
restAPIRequestsLimit: 10
```

## 🖥️ restAPILimitRefillPeriod

Default: ``30``<br>
restAPIRequestsLimit Sets the time limit is reset.<br>

Example
```yaml
# If restAPIRequestsLimit value is 5..
# Limit 5 refreshes in 30 seconds
restAPILimitRefillPeriod: 30

# Limit 5 refreshes in 1 minute
restAPILimitRefillPeriod: 60

# Limit 5 refreshes in 5 minutes
restAPILimitRefillPeriod: 300
```

## 💀 skiplimit

Default: ``10``<br>
Limits the number of wave skips caused by the ``/vote skip`` command.<br>
Avoid putting a very high load on the server by entering a wave skip that is too high.

Example
```yaml
# Maximum of 10 waves per vote
skiplimit: 10

# Maximum of 15 waves per vote
skiplimit: 15
```

## ⚔️ pvpAutoTeam

Default: ``true``<br>
Teams are automatically deployed according to the player's record when connecting to the PvP server.<br>
Players with high win rates are more likely to be placed on teams with many players with very low win rates.

Example
```yaml
# Enable PvP balance matching system
pvpAutoTeam: true

# Disable PvP balance matching system
pvpAutoTeam: false
```

# Ban

These are functions for connecting between servers.
Rework is in progress.

## shareBanListServer

Default: ``127.0.0.1``<br>
Connect with another server.<br>
In the connected state, you can use the broadcast command to send messages to all servers.

# Security
Manages functions useful for catching Griefers.

## votekick

Default: ``false``<br>
Allows players to start voting by pressing the hammer icon in the player list.<br>
This feature has the risk of voting being neutralized by trolling users, Modified clients, or griefers, and there have been cases of abuse by Modified clients.<br>
**If you use this function, you agree that _any disputes between players and server security may be vulnerable_.**

Example
```yaml
# Enable quick voting from player list
votekick: true

# Disable quick voting from player list
votekick: false
```

## antiGrief

Default: ``false``<br>
Execute various experimental anti-grief functions.<br>
This includes prohibiting access between the same IP and detecting power supply cutoff, which may cause malfunction or instability.

Example
```yaml
# Enable anti-grief system
antiGrief: true

# Disable anti-grief system
antiGrief: false
```

## minimumName

Default: ``false``<br>
Set the player's minimum nickname length to at least 4 characters.<br>
Very short nicknames prevent duplicate nicknames from being detected and hindered in quickly blocking the griefer.

Example
```yaml
# Set minimum nickname length to 4 characters
minimumName: true

# Unlimit nickname length
minimumName: false
```

## blockfooclient

Default: ``false``<br>
Blocking foo's client.
If this value is set to false, various cheats, unfair games, unintentional block place, and unexpected plugin errors cannot be prevented.<br>
If you set this value to false, **you agree that the plugin creator will not have any problems with plugin errors and server operation**.

Example
```yaml
# Block foo's client
blockfooclient: true

# Allow foo's client
blockfooclient: false
```

## allowMobile

Default: ``true``<br>
Allow mobile user access.<br>
Setting this value to false will prevent mobile players from entering the server.<br>

Example
```yaml
# Allow mobile players
allowMobile: true

# Block mobile players
allowMobile: false
```

## blockNewUser

Default: ``false``<br>
Blocks new users from entering the server.<br>
This feature is useful for malicious users to bypass bans using custom clients or VPNs.<br>
Enable this function to block modified client attacks while maintaining the authType value as ``None``.

Example
```yaml
# Block new players
blockNewUser: true

# Allow join new players
blockNewUser: false
```

# Discord

Settings related to Discord integration

## botToken
Enter the Bot ID.<br>
You can find it in the Discord Developer Center.

Example
```yaml
botToken: avqnwfe561awe4fvq
```

## channelToken
Enter the ID of the specific channel the bot will be used on.<br>
You will need to enable developer mode in your Discord settings to see this.

Example
```yaml
channelToken: 527319715439417593
```

## discordURL
Enter your discord invite URL.<br>
By entering your Discord URL here, you can use the ``/discord`` command to open the Discord URL from within the game.

Example
```yaml
discordURL: https://discord.gg/invitelink
```

## banChannelToken
Enter the ID of the specific channel the bot will be used on.<br>
When a player is banned on the server, the information of the banned player is displayed through the set Discord channel.

Example
```yaml
banChannelToken: 91475341795341759
```