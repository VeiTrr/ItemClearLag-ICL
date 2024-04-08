package vt.icl.commands;

import com.mojang.brigadier.CommandDispatcher;
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

public class IclCommand {
    private static final SuggestionProvider<ServerCommandSource> CONFIG_FIELDS = (context, builder) -> {
        Configuration config = ICL.config;
        config.get().forEach((key, type) -> {
            builder.suggest(key);
        });
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

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("icl")
                .then(CommandManager.literal("forceclean")
                        .executes(context -> {
                            forceClean(context.getSource().getServer(), context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("reload")
                        .executes(context -> {
                            reloadIcl(context.getSource().getPlayer());
                            return 1;
                        }))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("key", StringArgumentType.string())
                                        .suggests(CONFIG_FIELDS)
                                        .then(CommandManager.argument("value", StringArgumentType.string())
                                                .suggests((context, suggestionsBuilder) -> switch (StringArgumentType.getString(context, "key")) {
                                                    case "delay" -> suggestionsBuilder.suggest("80").buildFuture();
                                                    case "notificationdelay" ->
                                                            suggestionsBuilder.suggest("15").buildFuture();
                                                    case "notificationstart" ->
                                                            suggestionsBuilder.suggest("60").buildFuture();
                                                    case "notificationtimes" ->
                                                            suggestionsBuilder.suggest("4").buildFuture();
                                                    case "countdownstart" ->
                                                            suggestionsBuilder.suggest("5").buildFuture();
                                                    case "notificationcountdown", "showNotificationSound",
                                                         "showNotification" ->
                                                            suggestionsBuilder.suggest("true").suggest("false").buildFuture();
                                                    case "notificationSound" ->
                                                            SOUNDS.getSuggestions(context, suggestionsBuilder);
                                                    case "notificationLang" ->
                                                            LANGUAGES.getSuggestions(context, suggestionsBuilder);
                                                    default -> suggestionsBuilder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String value = StringArgumentType.getString(context, "value");
                                                    ConfigEdit(key, value, context.getSource().getPlayer());
                                                    return 1;
                                                }))))));
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

    public static void forceClean(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        ICL.clearItems(server);
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.forceclear")).formatted(Formatting.GREEN));
        }
    }

    public static void ConfigEdit(String key, String value, @Nullable ServerPlayerEntity player) {
        ICL.config.set(key, value);
        if (key.equals("notificationLang")) {
            ICL.translations = IclTranslationManager.loadTranslation(value);
        }
        if (player != null) {
            player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.config.updated", key, value)).formatted(Formatting.GREEN));
        }
    }
}
