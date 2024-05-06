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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import vt.icl.ICL;
import vt.icl.config.Configuration;
import vt.icl.config.lang.IclTranslationManager;

import java.util.concurrent.ExecutionException;

import static net.minecraft.command.suggestion.SuggestionProviders.AVAILABLE_SOUNDS;
import static vt.icl.ICL.IclTranslate;
import static vt.icl.ICL.config;

public class IclCommand {
    private static final SuggestionProvider<ServerCommandSource> CONFIG_FIELDS = (context, builder) -> {
        Configuration config = ICL.config;
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
        dispatcher.register(CommandManager.literal("icl")
                .executes(context -> {
                    showReadmeInfo(context.getSource().getPlayer());
                    return 1;
                })
                .then(CommandManager.literal("forceclean")
                        .requires(source -> !config.RequireOp || source.hasPermissionLevel(2))
                        .executes(context -> {
                            forceClean(context.getSource().getServer(), context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("reload")
                        .requires(source -> !config.RequireOp || source.hasPermissionLevel(2))
                        .executes(context -> {
                            reloadIcl(context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("cancel")
                        .requires(source -> !config.RequireOpCancel || source.hasPermissionLevel(2))
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
                        .requires(source -> !config.RequireOp || source.hasPermissionLevel(2))
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
                                                         "RequireOpCancel" ->
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

    public static void showReadmeInfo(@Nullable ServerPlayerEntity player) {
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.readme")).formatted(Formatting.GREEN));
        } else {
            ICL.LOGGER.info(IclTranslate("text.icl.readme"));
        }
    }

    public static void showConfigValue(String key, @Nullable ServerPlayerEntity player) {
        String currentValue = ICL.config.getValue(key);
        String defaultValue = new Configuration().getValue(key);
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.config.current", key, currentValue, defaultValue)).formatted(Formatting.GREEN));
        } else {
            ICL.LOGGER.info("Current value of {} is {}", key, currentValue);
            ICL.LOGGER.info("Default value of {} is {}", key, defaultValue);
        }

    }

    public static void reloadIcl(@Nullable ServerPlayerEntity player) {
        try {
            ICL.reloadIcl();
            if (player != null) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.reload")).formatted(Formatting.GREEN));
            }
        } catch (Exception e) {
            if (player != null) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.reload.fail")).formatted(Formatting.RED));
                player.sendMessage(Text.literal("[ICL] " + e.getMessage()).formatted(Formatting.RED));
            }
            ICL.LOGGER.error(e.getMessage());
        }

    }

    public static void cancelClean(@Nullable ServerPlayerEntity player, int seconds) {
        try {
            if (seconds < 0) {
                seconds = 0;
            }
            ICL.CancelIcl(seconds);
            if (player != null) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.cancel.message")).formatted(Formatting.GREEN));
            }
        } catch (Exception e) {
            if (player != null) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.reload.fail")).formatted(Formatting.RED));
                player.sendMessage(Text.literal("[ICL] " + e.getMessage()).formatted(Formatting.RED));
            }
            ICL.LOGGER.error(e.getMessage());
        }
    }

    public static void forceClean(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        ICL.clearItems(server);
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.forceclear")).formatted(Formatting.GREEN));
        }
    }

    public static void ConfigEdit(String key, String value, @Nullable ServerPlayerEntity player) {
        ICL.config.set(key, value);
        if (key.equals("NotificationLang")) {
            ICL.translations = IclTranslationManager.loadTranslation(value);
        }
        if (key.equals("NotificationColor")) {
            if (value != null) {
                try {
                    if (player != null) {
                        player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.config.updated", key, value)).formatted(Formatting.valueOf(config.NotificationColor)));
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    if (player != null) {
                        player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.config.updated", key, value)).formatted(Formatting.RED));
                        return;
                    }
                }
            }
        }
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.config.updated", key, value)).formatted(Formatting.GREEN));
        }
        ICL.reloadIcl();
    }

}