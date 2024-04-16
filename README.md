[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1001584?style=for-the-badge&logo=curseforge&logoColor=%230d0d0d&labelColor=%23f16436&color=%230d0d0d)](https://www.curseforge.com/minecraft/mc-mods/itemclearlag) [![Modrinth Downloads](https://img.shields.io/modrinth/dt/NJcJEXNc?style=for-the-badge&logo=modrinth&color=%231bd96a)](https://modrinth.com/mod/itemclearlag)

# ItemClearLag (ICL)

ItemClearLag (ICL) is a Minecraft mod designed to improve server performance by periodically removing items from the ground. This mod is especially useful for servers with high player counts where dropped items can accumulate and cause lag.

## Installation

1. Ensure you have Fabric installed.
2. Download the latest version of ICL from the [releases page](https://github.com/VeiTrr/ItemClearLag-ICL/releases).
3. Place the downloaded .jar file into your `mods` folder.
4. Start your server or game.

## Commands

Write `/icl` in chat to see all available commands.

The main command provided by ICL is `/icl`, which has several subcommands:

- `/icl forceclean`: Immediately clears all items on the ground.
- `/icl reload`: Reloads the ICL.
- `/icl config set <key> <value>`: Changes a configuration value. The changes are immediately saved to disk.

## Configuration

Configuration values can be changed using the `/icl config set` command. Here are some of the configurable values:

- `Delay`: The delay (in seconds) between automatic item clears.
- `NotificationDelay`: The delay (in seconds) before a clear when a notification will be sent.
- `NotificationStart`: The time (in seconds) when notifications start being sent before a clear.
- `NotificationTimes`: The number of notifications to send before a clear.
- `CountdownStart`: The time (in seconds) when the countdown starts before a clear.
- `doNotificationCountdown`: Whether to show a countdown before a clear.
- `doNotificationSound`: Whether to play a sound when a notification is sent.
- `doLastNotificationSound`: Whether to play a sound when a last notification is sent.
- `NotificationSound`: The sound to play when a notification is sent.
- `LastNotificationSound`: The sound to play when a last notification is sent.
- `NotificationLang`: The language for notifications.
- `NotificationColor`: The color for notifications.

## License

ICL is licensed under the MIT License. See the `LICENSE` file for more details.
