# ItemClearLag (ICL)

ItemClearLag (ICL) is a Minecraft mod designed to improve server performance by periodically removing items from the ground. This mod is especially useful for servers with high player counts where dropped items can accumulate and cause lag.

## Installation

1. Ensure you have Fabric installed.
2. Download the latest version of ICL from the [releases page](https://github.com/VeiTrr/ItemClearLag-ICL/releases).
3. Place the downloaded .jar file into your `mods` folder.
4. Start your server or game.

## Commands

The main command provided by ICL is `/icl`, which has several subcommands:

- `/icl forceclean`: Immediately clears all items on the ground.
- `/icl reload`: Reloads the ICL.
- `/icl config set <key> <value>`: Changes a configuration value. The changes are immediately saved to disk.

## Configuration

Configuration values can be changed using the `/icl config set` command. Here are some of the configurable values:

- `delay`: The delay (in seconds) between automatic item clears.
- `notificationdelay`: The delay (in seconds) before a clear when a notification will be sent.
- `notificationstart`: The time (in seconds) when notifications start being sent before a clear.
- `notificationtimes`: The number of notifications to send before a clear.
- `countdownstart`: The time (in seconds) when the countdown starts before a clear.
- `notificationcountdown`: Whether to show a countdown before a clear.
- `showNotificationSound`: Whether to play a sound when a notification is sent.
- `showNotification`: Whether to show notifications.
- `notificationSound`: The sound to play when a notification is sent.
- `notificationLang`: The language for notifications.

## License

ICL is licensed under the MIT License. See the `LICENSE` file for more details.