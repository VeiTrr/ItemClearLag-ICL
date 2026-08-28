package vt.icl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vt.icl.config.ConfigManager;
import vt.icl.config.Configuration;
import vt.icl.config.lang.IclTranslationManager;
import vt.icl.mixin.ItemEntityAccessor;
import vt.icl.permission.PermissionHandler;
import vt.icl.text.LegacyText;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static vt.icl.config.lang.IclTranslationManager.createDefaultTranslationFiles;

public class ICLCommon {
    public static final String MOD_ID = "icl";
    private static final String DEFAULT_NOTIFICATION_SOUND = "block.note_block.harp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID.toUpperCase());
    public static final Path CONFIG_DIR = new File("./config/" + MOD_ID.substring(0, 1).toUpperCase() + MOD_ID.substring(1)).toPath();
    public static Configuration config = ConfigManager.getConfig();
    private static Timer TIMER = new Timer(MOD_ID.toUpperCase());
    public static Map<String, String> translations;
    private static Map<String, String> defaultTranslations;
    public static PermissionHandler permissionHandler;
    private static MinecraftServer server;

    public static void init() {
        LOGGER.info("Initializing " + MOD_ID.toUpperCase());
        createDefaultTranslationFiles();
        translations = IclTranslationManager.loadTranslation(config.NotificationLang);
        defaultTranslations = IclTranslationManager.loadTranslation("en_us");
    }

    public static void onServerStart(MinecraftServer server) {
        ICLCommon.server = server;
        resetTimer();
        if (config.Delay > 0) {
            doItemClean(server);
            if (config.doShowNotification) {
                setupNotificationTimers(server);
            }
            if (config.doNotificationCountdown) {
                setupCountdownTimer(server);
            }
        } else {
            LOGGER.info(MOD_ID.toUpperCase() + " disabled, delay is less than 0");
        }
        LOGGER.info(MOD_ID.toUpperCase() + " initialized");
    }

    public static void onServerStop() {
        TIMER.cancel();
        LOGGER.info(MOD_ID.toUpperCase() + " stopped");
    }

    private static void resetTimer() {
        TIMER.cancel();
        TIMER.purge();
        TIMER = new Timer(MOD_ID.toUpperCase());
    }

    public static void doItemClean(MinecraftServer server) {
        long delay = ICLCommon.config.Delay;
        if (delay < 0) {
            return;
        }
        scheduleTask(delay * 1000, () -> {
            if (ICLCommon.config.doShowNotification) {
                setupNotificationTimers(server);
            }
            if (ICLCommon.config.doNotificationCountdown) {
                setupCountdownTimer(server);
            }

            server.execute(() -> runSafely("clear items", () -> clearItems(server)));

            ICLCommon.TIMER.purge();
            doItemClean(server);
        });
    }

    public static void setupNotificationTimers(MinecraftServer server) {
        for (int i = 0; i < ICLCommon.config.NotificationTimes; i++) {
            int finalI = i;
            long delay = ICLCommon.config.Delay - ICLCommon.config.NotificationStart + ICLCommon.config.NotificationDelay * i;
            if (delay < 0 || delay > ICLCommon.config.Delay) {
                continue;
            }
            scheduleTask(delay * 1000, () -> {
                ICLCommon.LOGGER.info("{} seconds left", "Clearing items " + (ICLCommon.config.NotificationStart - ICLCommon.config.NotificationDelay * finalI));
                for (var player : server.getPlayerManager().getPlayerList()) {
                    MutableText message = IclText(IclTranslate("text.icl.notification", (ICLCommon.config.NotificationStart - ICLCommon.config.NotificationDelay * finalI)) + " ",
                            notificationFormatting());
                    IclMessage(player, message);
                    if (ICLCommon.config.doNotificationSound) {
                        runSafely("notification sound", () -> IclPlaysound(player, false));
                    }
                }
            });
        }
    }

    private static void IclMessage(ServerPlayerEntity player, MutableText message) {
        if (permissionCheckforCancel(player.getCommandSource())) {
            message.append(LegacyText.parse(IclTranslate("text.icl.cancel.button"), Formatting.RED)
                    .styled(style -> style.withClickEvent(IclCancelEvent())));
        }
        player.sendMessage(message);
    }

    public static void setupCountdownTimer(MinecraftServer server) {
        long countdownstart = ICLCommon.config.CountdownStart;
        if (countdownstart > ICLCommon.config.Delay) {
            countdownstart = ICLCommon.config.Delay;
        }
        long delay = ICLCommon.config.Delay - countdownstart;
        if (delay < 0 || delay > ICLCommon.config.Delay) {
            return;
        }
        if (countdownstart < 0) {
            return;
        }


        long finalCountdownstart = countdownstart;
        scheduleTask(delay * 1000, () -> {
            for (int i = 0; i < finalCountdownstart; i++) {
                int finalI = i;
                scheduleTask(finalI * 1000L, () -> {
                    ICLCommon.LOGGER.info("{} seconds left", "Clearing items " + (finalCountdownstart - finalI));
                    for (var player : server.getPlayerManager().getPlayerList()) {
                        MutableText message = IclText(IclTranslate("text.icl.countdown", (finalCountdownstart - finalI)) + " ",
                                notificationFormatting());
                        IclMessage(player, message);
                    }
                });
            }
        });
    }

    public static void clearItems(MinecraftServer server) {
        ICLCommon.LOGGER.info("Clearing items");
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (ICLCommon.config.doShowNotification) {
                player.sendMessage(IclText(IclTranslate("text.icl.clear"), notificationFormatting()));
                if (ICLCommon.config.doLastNotificationSound) {
                    runSafely("last notification sound", () -> IclPlaysound(player, true));
                }
            }
        }
        int count = 0;
        for (var world : server.getWorlds()) {
            for (var entity : world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), Entity::isAlive)) {
                if (ICLCommon.config.preserveNoPickupItems) {
                    ItemEntityAccessor accessor = (ItemEntityAccessor) entity;
                    if (accessor.getPickupDelay() == Short.MAX_VALUE) {
                        continue;
                    }
                }
                if (ICLCommon.config.preserveNoDespawnItems) {
                    if (entity.getItemAge() == Short.MIN_VALUE) {
                        continue;
                    }
                }
                count += entity.getStack().getCount();
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (ICLCommon.config.doShowNotification) {
                player.sendMessage(IclText(IclTranslate("text.icl.clear.finish", count), notificationFormatting()));
            }
        }
        ICLCommon.LOGGER.info("Items cleared: {}", count);
    }

    public static void reloadIcl() {
        resetTimer();
        ICLCommon.config = ConfigManager.getConfig();
        if (ICLCommon.config.Delay > 0) {
            doItemClean(ICLCommon.server);
            if (ICLCommon.config.doShowNotification) {
                setupNotificationTimers(ICLCommon.server);
            }
            if (ICLCommon.config.doNotificationCountdown) {
                setupCountdownTimer(ICLCommon.server);
            }
        } else {
            ICLCommon.LOGGER.info(ICLCommon.MOD_ID.toUpperCase() + " disabled, delay is less than 0");
        }
    }

    public static void CancelIcl(int tempDelay) {
        resetTimer();
        if (tempDelay > 0) {
            scheduleTask(tempDelay * 1000L, () -> {
                ICLCommon.config = ConfigManager.getConfig();
                if (ICLCommon.config.Delay > 0) {
                    doItemClean(ICLCommon.server);
                    if (ICLCommon.config.doShowNotification) {
                        setupNotificationTimers(ICLCommon.server);
                    }
                    if (ICLCommon.config.doNotificationCountdown) {
                        setupCountdownTimer(ICLCommon.server);
                    }
                } else {
                    ICLCommon.LOGGER.info(ICLCommon.MOD_ID.toUpperCase() + " disabled, delay is less than 0");
                }
            });
        } else {
            ICLCommon.config = ConfigManager.getConfig();
            if (ICLCommon.config.Delay > 0) {
                doItemClean(ICLCommon.server);
                if (ICLCommon.config.doShowNotification) {
                    setupNotificationTimers(ICLCommon.server);
                }
                if (ICLCommon.config.doNotificationCountdown) {
                    setupCountdownTimer(ICLCommon.server);
                }
            } else {
                ICLCommon.LOGGER.info(ICLCommon.MOD_ID.toUpperCase() + " disabled, delay is less than 0");
            }
        }
    }

    public static ClickEvent IclCancelEvent() {
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/icl cancel");
    }

    /**
     * Wraps a message in the configured prefix and renders the legacy codes in
     * both. {@code base} colours whatever the strings do not colour themselves,
     * and is what a {@code &r} in them returns to.
     */
    public static MutableText IclText(String message, Formatting base) {
        return LegacyText.parse(notificationPrefix() + message, base);
    }

    private static String notificationPrefix() {
        String prefix = ICLCommon.config.NotificationPrefix;
        return prefix == null ? Configuration.DEFAULT_NOTIFICATION_PREFIX : prefix;
    }

    public static String IclTranslate(String key, Object... args) {
        String translation = null;
        if (ICLCommon.translations != null) {
            translation = ICLCommon.translations.get(key);
            ICLCommon.LOGGER.debug("Translation: {}", translation);
        }
        if (translation == null && ICLCommon.defaultTranslations != null) {
            translation = ICLCommon.defaultTranslations.get(key);
            ICLCommon.LOGGER.debug("Default Translation: {}", translation);
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
        Vec3d vec3d = safeSoundPosition(player);
        Identifier sound = resolveSoundIdentifier(isLastSound);
        RegistryEntry<SoundEvent> registryEntry = RegistryEntry.of(SoundEvent.of(sound));
        player.networkHandler.send(new PlaySoundS2CPacket(registryEntry,
                SoundCategory.PLAYERS, vec3d.getX(), vec3d.getY(), vec3d.getZ(), 1, 1, 1), (PacketCallbacks)null);
    }

    private static void scheduleTask(long delayMillis, Runnable action) {
        ICLCommon.TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                runSafely("timer task", action);
            }
        }, delayMillis);
    }

    private static void runSafely(String action, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            ICLCommon.LOGGER.error("ICL {} failed", action, e);
        }
    }

    private static Formatting notificationFormatting() {
        try {
            return Formatting.valueOf(ICLCommon.config.NotificationColor);
        } catch (IllegalArgumentException | NullPointerException e) {
            ICLCommon.LOGGER.error("Invalid notification color '{}', falling back to RED", ICLCommon.config.NotificationColor);
            return Formatting.RED;
        }
    }

    private static Identifier resolveSoundIdentifier(boolean isLastSound) {
        String sound = isLastSound ? ICLCommon.config.LastNotificationSound : ICLCommon.config.NotificationSound;
        try {
            return Identifier.of(sound);
        } catch (IllegalArgumentException | NullPointerException e) {
            ICLCommon.LOGGER.error("Invalid notification sound '{}', falling back to {}", sound, DEFAULT_NOTIFICATION_SOUND);
            return Identifier.of(DEFAULT_NOTIFICATION_SOUND);
        }
    }

    private static Vec3d safeSoundPosition(ServerPlayerEntity player) {
        double e = player.getX();
        double f = player.getY();
        double g = player.getZ();
        double h = e * e + f * f + g * g;
        if (h == 0) {
            return player.getPos();
        }
        double k = Math.sqrt(h);
        return new Vec3d(player.getX() + e / k * 2.0, player.getY() + f / k * 2.0, player.getZ() + g / k * 2.0);
    }

    private static boolean permissionCheckforCancel(ServerCommandSource source) {
        if (ICLCommon.permissionHandler != null) {
            return ICLCommon.permissionHandler.hasPermission(source, ICLCommon.MOD_ID + "." + "cancel");
        } else {
            return !ICLCommon.config.RequireOpCancel || source.hasPermissionLevel(2);
        }
    }
}
