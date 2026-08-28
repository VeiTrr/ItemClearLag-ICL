package vt.icl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import vt.icl.ICLCommon;
import vt.icl.config.Configuration;
import vt.icl.config.lang.IclTranslationManager;

import java.util.concurrent.ExecutionException;

import static net.minecraft.command.suggestion.SuggestionProviders.AVAILABLE_SOUNDS;
import static vt.icl.ICLCommon.IclTranslate;
import static vt.icl.ICLCommon.config;

public class IclCommand {
    private static final SuggestionProvider<ServerCommandSource> CONFIG_FIELDS = (context, builder) -> {
        Configuration config = ICLCommon.config;
        config.get().forEach((key, type) -> builder.suggest(key));
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> LANGUAGES = (context, builder) -> {
        IclTranslationManager.getAvailableLangs().forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> SOUNDS = (context, builder) -> {
        try {
            AVAILABLE_SOUNDS.getSuggestions(context, builder).get().getList().forEach(suggestion -> {
                String sound = suggestion.getText();
                String[] split = sound.split(":");
                builder.suggest(split[split.length - 1]);
            });
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> COLORS = (context, builder) -> {
        builder.suggest("BLACK");
        builder.suggest("DARK_BLUE");
        builder.suggest("DARK_GREEN");
        builder.suggest("DARK_AQUA");
        builder.suggest("DARK_RED");
        builder.suggest("DARK_PURPLE");
        builder.suggest("GOLD");
        builder.suggest("GRAY");
        builder.suggest("DARK_GRAY");
        builder.suggest("BLUE");
        builder.suggest("GREEN");
        builder.suggest("AQUA");
        builder.suggest("RED");
        builder.suggest("LIGHT_PURPLE");
        builder.suggest("YELLOW");
        builder.suggest("WHITE");
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        register(dispatcher);
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("icl")
                .executes(context -> {
                    showReadmeInfo(context.getSource().getPlayer());
                    return 1;
                })
                .then(CommandManager.literal("forceclean")
                        .requires(source -> permissionCheck(source, "forceclean"))
                        .executes(context -> {
                            forceClean(context.getSource().getServer(), context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("reload")
                        .requires(source -> permissionCheck(source, "reload"))
                        .executes(context -> {
                            reloadIcl(context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("cancel")
                        .requires(source -> permissionCheckforCancel(source))
                        .executes(context -> {
                            cancelClean(context.getSource().getPlayer(), 0);
                            return 1;
                        }).then(CommandManager.argument("seconds", IntegerArgumentType.integer())
                                .suggests((context, suggestionsBuilder) -> suggestionsBuilder.suggest("300").buildFuture())
                                .executes(context -> {
                                    cancelClean(context.getSource().getPlayer(), IntegerArgumentType.getInteger(context, "seconds"));
                                    return 1;
                                })))
                .then(CommandManager.literal("config")
                        .requires(source -> permissionCheck(source, "config"))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .suggests(CONFIG_FIELDS)
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            showConfigValue(key, context.getSource().getPlayer());
                                            return 1;
                                        })
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .suggests((context, suggestionsBuilder) -> switch (StringArgumentType.getString(context, "key")) {
                                                    case "Delay" -> suggestionsBuilder.suggest("80").buildFuture();
                                                    case "NotificationDelay" ->
                                                            suggestionsBuilder.suggest("15").buildFuture();
                                                    case "NotificationStart" ->
                                                            suggestionsBuilder.suggest("60").buildFuture();
                                                    case "NotificationTimes" ->
                                                            suggestionsBuilder.suggest("4").buildFuture();
                                                    case "CountdownStart" ->
                                                            suggestionsBuilder.suggest("5").buildFuture();
                                                    case "doNotificationCountdown", "doShowNotification",
                                                         "doNotificationSound", "doLastNotificationSound", "RequireOp",
                                                         "RequireOpCancel", "preserveNoDespawnItems",
                                                         "preserveNoPickupItems", "UsePermissionsApi" ->
                                                            suggestionsBuilder.suggest("true").suggest("false").buildFuture();
                                                    case "NotificationSound", "LastNotificationSound" ->
                                                            SOUNDS.getSuggestions(context, suggestionsBuilder);
                                                    case "NotificationLang" ->
                                                            LANGUAGES.getSuggestions(context, suggestionsBuilder);
                                                    case "NotificationColor" ->
                                                            COLORS.getSuggestions(context, suggestionsBuilder);
                                                    default -> suggestionsBuilder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String value = StringArgumentType.getString(context, "value");
                                                    ConfigEdit(key, value, context.getSource().getPlayer());
                                                    return 1;
                                                }))))));
    }

    private static boolean permissionCheck(ServerCommandSource source, String permission) {
        if (ICLCommon.permissionHandler != null) {
            return ICLCommon.permissionHandler.hasPermission(source, ICLCommon.MOD_ID + "." + permission);
        } else {
            return !config.RequireOp || source.hasPermissionLevel(2);
        }
    }

    private static boolean permissionCheckforCancel(ServerCommandSource source) {
        if (ICLCommon.permissionHandler != null) {
            return ICLCommon.permissionHandler.hasPermission(source, ICLCommon.MOD_ID + "." + "cancel");
        } else {
            return !config.RequireOpCancel || source.hasPermissionLevel(2);
        }
    }

    public static void showReadmeInfo(@Nullable ServerPlayerEntity player) {
        if (player != null) {
            player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.readme"), Formatting.GREEN));
        } else {
            ICLCommon.LOGGER.info(IclTranslate("text.icl.readme"));
        }
    }

    public static void showConfigValue(String key, @Nullable ServerPlayerEntity player) {
        String currentValue = ICLCommon.config.getValue(key);
        String defaultValue = new Configuration().getValue(key);
        if (player != null) {
            player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.config.current", key, currentValue, defaultValue), Formatting.GREEN));
        } else {
            ICLCommon.LOGGER.info("Current value of {} is {}", key, currentValue);
            ICLCommon.LOGGER.info("Default value of {} is {}", key, defaultValue);
        }

    }

    public static void reloadIcl(@Nullable ServerPlayerEntity player) {
        try {
            ICLCommon.reloadIcl();
            if (player != null) {
                player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.reload"), Formatting.GREEN));
            }
        } catch (Exception e) {
            if (player != null) {
                player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.reload.fail"), Formatting.RED));
                player.sendMessage(ICLCommon.IclText(e.getMessage(), Formatting.RED));
            }
            ICLCommon.LOGGER.error(e.getMessage());
        }

    }

    public static void cancelClean(@Nullable ServerPlayerEntity player, int seconds) {
        try {
            if (seconds < 0) {
                seconds = 0;
            }
            ICLCommon.CancelIcl(seconds);
            if (player != null) {
                player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.cancel.message"), Formatting.GREEN));
            }
        } catch (Exception e) {
            if (player != null) {
                player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.reload.fail"), Formatting.RED));
                player.sendMessage(ICLCommon.IclText(e.getMessage(), Formatting.RED));
            }
            ICLCommon.LOGGER.error(e.getMessage());
        }
    }

    public static void forceClean(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        server.execute(() -> ICLCommon.clearItems(server));
        if (player != null) {
            player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.forceclear"), Formatting.GREEN));
        }
    }

    public static void ConfigEdit(String key, String value, @Nullable ServerPlayerEntity player) {
        ICLCommon.config.set(key, value);
        if (key.equals("NotificationLang")) {
            ICLCommon.translations = IclTranslationManager.loadTranslation(value);
        }
        if (key.equals("NotificationColor")) {
            if (value != null) {
                try {
                    if (player != null) {
                        player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.config.updated", key, value), Formatting.valueOf(config.NotificationColor)));
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    if (player != null) {
                        player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.config.updated", key, value), Formatting.RED));
                        return;
                    }
                }
            }
        }
        if (player != null) {
            player.sendMessage(ICLCommon.IclText(IclTranslate("text.icl.config.updated", key, value), Formatting.GREEN));
        }
        ICLCommon.reloadIcl();
    }

}