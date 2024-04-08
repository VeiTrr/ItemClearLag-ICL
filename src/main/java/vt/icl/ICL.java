package vt.icl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
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
        translations = IclTranslationManager.loadTranslation(config.notificationLang);
        defaultTranslations = IclTranslationManager.loadTranslation("en_us");
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ICL.server = server;
            translations = IclTranslationManager.loadTranslation(config.notificationLang);
            if (config.delay > 0) {
                doItemClean(server);
                if (config.showNotification) {
                    setupNotificationTimers(server);
                }
                if (config.notificationcountdown) {
                    setupCountdownTimer(server);
                }
            } else {
                LOGGER.info("ICL disabled, delay is less than 0");
            }
            LOGGER.info("ICL initialized");
        });
    }

    public static void doItemClean(MinecraftServer server) {
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {

                if (config.showNotification) {
                    setupNotificationTimers(server);
                }
                if (config.notificationcountdown) {
                    setupCountdownTimer(server);
                }

                clearItems(server);

                TIMER.purge();

                doItemClean(server);
            }
        }, config.delay * 1000);
    }

    private static void setupNotificationTimers(MinecraftServer server) {
        for (int i = 0; i < config.notificationtimes; i++) {
            int finalI = i;
            TIMER.schedule(new TimerTask() {
                @Override
                public void run() {
                    LOGGER.info("{} seconds left", "Clearing items " + (config.notificationstart - config.notificationdelay * finalI));
                    for (var player : server.getPlayerManager().getPlayerList()) {
                        player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.notification", (config.notificationstart - config.notificationdelay * finalI)))
                                .formatted(Formatting.RED));
                        try {
                            if (config.showNotificationSound) {
                                IclPlaysound(player);
                            }
                        } catch (Exception e) {
                            player.sendMessage(Text.literal(e.getMessage()).formatted(Formatting.RED));
                            LOGGER.error("Failed to play sound: " + e.getMessage());
                        }
                    }
                }
            }, (config.delay - config.notificationstart + config.notificationdelay * i) * 1000);
        }
    }

    private static void setupCountdownTimer(MinecraftServer server) {
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < config.countdownstart; i++) {
                    int finalI = i;
                    TIMER.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            LOGGER.info("{} seconds left", "Clearing items " + (config.countdownstart - finalI));
                            for (var player : server.getPlayerManager().getPlayerList()) {
                                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.countdown", (config.countdownstart - finalI)))
                                        .formatted(Formatting.RED));
                            }
                        }
                    }, finalI * 1000L);
                }
            }
        }, (config.delay - config.countdownstart) * 1000);
    }

    public static void clearItems(MinecraftServer server) {
        LOGGER.info("Clearing items");
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (config.showNotification) {
                player.sendMessage(Text.literal("[ICL] " + IclTranslate("text.icl.clear")).formatted(Formatting.RED));
            }
        }
        for (var world : server.getWorlds()) {
            world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), Entity::isAlive).forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
        }
    }

    public static void reloadIcl() {
        TIMER.cancel();
        TIMER = new Timer("ICL");
        config = ConfigManager.getConfig();
        if (config.delay > 0) {
            doItemClean(server);
            if (config.showNotification) {
                setupNotificationTimers(server);
            }
            if (config.notificationcountdown) {
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

    public static void IclPlaysound(ServerPlayerEntity player) {
        Vec3d vec3d;
        float j;
        double e = player.getX();
        double f = player.getY();
        double g = player.getZ();
        double h = e * e + f * f + g * g;
        vec3d = new Vec3d(e, f, g);
        j = 1.0F;
        double k = Math.sqrt(h);
        vec3d = new Vec3d(player.getX() + e / k * 2.0, player.getY() + f / k * 2.0, player.getZ() + g / k * 2.0);
        player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(new Identifier(config.notificationSound), SoundCategory.PLAYERS, vec3d, 1,1, player.getWorld().getRandom().nextLong()));
    }

}
