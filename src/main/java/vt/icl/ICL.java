package vt.icl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vt.icl.commands.IclCommand;
import vt.icl.config.ConfigManager;
import vt.icl.config.Configuration;
import vt.icl.config.lang.IclTranslationManager;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static vt.icl.config.lang.IclTranslationManager.createDefaultTranslationFiles;

public class ICL implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ICL");
    public static Configuration config = ConfigManager.getConfig();
    private static Timer TIMER = new Timer("ICL");
    private static MinecraftServer server;

    public static Map<String, String> translations;
    private static Map<String, String> defaultTranslations;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ICL");
        CommandRegistrationCallback.EVENT.register(IclCommand::register);
        createDefaultTranslationFiles();
        translations = IclTranslationManager.loadTranslation(config.NotificationLang);
        defaultTranslations = IclTranslationManager.loadTranslation("en_us");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ICL.server = server;
            translations = IclTranslationManager.loadTranslation(config.NotificationLang);
            if (config.Delay > 0) {
                doItemClean(server);
                if (config.doShowNotification) {
                    setupNotificationTimers(server);
                }
                if (config.doNotificationCountdown) {
                    setupCountdownTimer(server);
                }
            } else {
                LOGGER.info("ICL disabled, delay is less than 0");
            }
            LOGGER.info("ICL initialized");
        });
    }

    public static void doItemClean(MinecraftServer server) {
        long delay = config.Delay;
        if (delay < 0) {
            return;
        }
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {

                if (config.doShowNotification) {
                    setupNotificationTimers(server);
                }
                if (config.doNotificationCountdown) {
                    setupCountdownTimer(server);
                }

                clearItems(server);

                TIMER.purge();

                doItemClean(server);
            }
        }, delay * 1000);
    }

    private static void setupNotificationTimers(MinecraftServer server) {
        for (int i = 0; i < config.NotificationTimes; i++) {
            int finalI = i;
            long delay = config.Delay - config.NotificationStart + config.NotificationDelay * i;
            if (delay < 0  || delay > config.Delay) {
                continue;
            }
            TIMER.schedule(new TimerTask() {
                @Override
                public void run() {
                    LOGGER.info("{} seconds left", "Clearing items " + (config.NotificationStart - config.NotificationDelay * finalI));
                    for (var player : server.getPlayerManager().getPlayerList()) {
                        player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.notification", (config.NotificationStart - config.NotificationDelay * finalI)))
                                .formatted(Formatting.valueOf(config.NotificationColor)));
                        try {
                            if (config.doNotificationSound) {
                                IclPlaysound(player, false);
                            }
                        } catch (Exception e) {
                            player.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.valueOf(config.NotificationColor)));
                            LOGGER.error("Failed to play sound: " + e.getMessage());
                        }
                    }
                }
            }, delay * 1000);
        }
    }

    private static void setupCountdownTimer(MinecraftServer server) {
        long countdownstart = config.CountdownStart;
        if (countdownstart > config.Delay) {
            countdownstart = config.Delay;
        }
        long delay = config.Delay - countdownstart;
        if (delay < 0 || delay > config.Delay) {
            return;
        }
        if (countdownstart < 0) {
            return;
        }


        long finalCountdownstart = countdownstart;
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < finalCountdownstart; i++) {
                    int finalI = i;
                    TIMER.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            LOGGER.info("{} seconds left", "Clearing items " + (finalCountdownstart - finalI));
                            for (var player : server.getPlayerManager().getPlayerList()) {
                                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.countdown", (finalCountdownstart - finalI)))
                                        .formatted(Formatting.valueOf(config.NotificationColor)));
                            }
                        }
                    }, finalI * 1000L);
                }
            }
        }, (delay) * 1000);
    }

    public static void clearItems(MinecraftServer server) {
        LOGGER.info("Clearing items");
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (config.doShowNotification) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.clear")).formatted(Formatting.valueOf(config.NotificationColor)));
                try {
                    if (config.doLastNotificationSound) {
                        IclPlaysound(player, true);
                    }
                } catch (Exception e) {
                    player.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.valueOf(config.NotificationColor)));
                    LOGGER.error("Failed to play sound: " + e.getMessage());
                }
            }
        }
        int count = 0;
        for (var world : server.getWorlds()) {
            for (var entity : world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), Entity::isAlive)) {
                count += entity.getStack().getCount();
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (config.doShowNotification) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.clear.finish", count)).formatted(Formatting.valueOf(config.NotificationColor)));
            }
        }
        LOGGER.info("Items cleared: {}", count);
    }

    public static void reloadIcl() {
        TIMER.cancel();
        TIMER = new Timer("ICL");
        config = ConfigManager.getConfig();
        if (config.Delay > 0) {
            doItemClean(server);
            if (config.doShowNotification) {
                setupNotificationTimers(server);
            }
            if (config.doNotificationCountdown) {
                setupCountdownTimer(server);
            }
        } else {
            LOGGER.info("ICL disabled, delay is less than 0");
        }
    }

    public static String IclTranslate(String key, Object... args) {
        String translation = null;
        if (translations != null) {
            translation = translations.get(key);
        }
        if (translation == null && defaultTranslations != null) {
            translation = defaultTranslations.get(key);
        }
        if (translation != null) {
            if (args != null && args.length > 0) {
                return String.format(translation, args);
            } else {
                return translation;
            }
        } else {
            return key;
        }
    }

    public static void IclPlaysound(ServerPlayerEntity player, boolean isLastSound) {
        Vec3d vec3d;
        double e = player.getX();
        double f = player.getY();
        double g = player.getZ();
        double h = e * e + f * f + g * g;
        double k = Math.sqrt(h);
        vec3d = new Vec3d(player.getX() + e / k * 2.0, player.getY() + f / k * 2.0, player.getZ() + g / k * 2.0);
        Identifier sound;
        if (isLastSound) {
            sound = new Identifier(config.LastNotificationSound);
        } else {
            sound = new Identifier(config.NotificationSound);
        }
        player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(sound, SoundCategory.PLAYERS, vec3d, 1, 1, 1));
    }

}
