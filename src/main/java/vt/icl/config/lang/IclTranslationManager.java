package vt.icl.config.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;
import vt.icl.ICL;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IclTranslationManager {
    private static final String LANG_FILE_NAME = "Icl/lang/%s.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    public static List<String> getAvailableLangs() {
        File langDir = FabricLoader.getInstance().getConfigDir().resolve("Icl/lang").toFile();
        String[] files = langDir.list((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                files[i] = files[i].substring(0, files[i].length() - 5);
            }
            return List.of(files);
        }
        return List.of();
    }

    public static Map<String, String> loadTranslation(String lang) {
        File langFile = FabricLoader.getInstance().getConfigDir().resolve(String.format(LANG_FILE_NAME, lang)).toFile();
        if (langFile.exists()) {
            try {
                String json = FileUtils.readFileToString(langFile, "UTF-8");
                Map<String, String> loadedTranslations = GSON.fromJson(json, MAP_TYPE);

                Map<String, String> defaultTranslations = getDefaultEnUsTranslations();

                for (String key : defaultTranslations.keySet()) {
                    if (!loadedTranslations.containsKey(key)) {
                        if (Objects.equals(lang, "ru_ru")) {
                            defaultTranslations = getDefaultRuRuTranslations();
                        }
                        loadedTranslations.put(key, defaultTranslations.get(key));
                        ICL.LOGGER.info("Warning: Missing translation for key '" + key + "' in " + lang + ". Using default translation.");
                    }
                }

                saveTranslation(lang, loadedTranslations);

                return loadedTranslations;
            } catch (IOException e) {
                ICL.LOGGER.info("Failed to load translation file for " + lang);
                ICL.LOGGER.info(e.getMessage());
            }
        }
        return null;
    }

    public static void saveTranslation(String lang, Map<String, String> translations) {
        File langFile = FabricLoader.getInstance().getConfigDir().resolve(String.format(LANG_FILE_NAME, lang)).toFile();
        try {
            String json = GSON.toJson(translations, MAP_TYPE);
            FileUtils.writeStringToFile(langFile, json, "UTF-8");
        } catch (IOException e) {
            ICL.LOGGER.info("Failed to save translation file for " + lang + ": " + e.getMessage());
        }
    }

    public static void createDefaultTranslationFiles() {
        createDefaultTranslationFile("en_us", getDefaultEnUsTranslations());
        createDefaultTranslationFile("ru_ru", getDefaultRuRuTranslations());
    }

    private static void createDefaultTranslationFile(String lang, Map<String, String> defaultTranslations) {
        File langFile = FabricLoader.getInstance().getConfigDir().resolve(String.format(LANG_FILE_NAME, lang)).toFile();
        if (!langFile.exists()) {
            saveTranslation(lang, defaultTranslations);
        }
    }

    private static Map<String, String> getDefaultEnUsTranslations() {
        Map<String, String> translations = new HashMap<>();
        translations.put("text.icl.notification", "Items on the ground will be removed in %d seconds");
        translations.put("text.icl.countdown", "Clearing items in %d");
        translations.put("text.icl.clear", "Clearing items");
        translations.put("text.icl.forceclear", "Forced clearing of items");
        translations.put("text.icl.config.updated", "Config value %s changed to %s");
        translations.put("text.icl.reload", "ICL reloaded");
        translations.put("text.icl.cancel.button", "[Cancel]");
        translations.put("text.icl.cancel.message", "Item clear cancelled");
        translations.put("text.icl.reload.fail", "Failed to reload ICL");
        translations.put("text.icl.clear.finish", "Items cleared: %d");
        translations.put("text.icl.config.current", "Current value of %s: %s (Default: %s)");
        translations.put("text.icl.readme", "§l§nCommands§r\n" +
                "\nThe main command provided by ICL is §a/icl§r, which has several subcommands:\n" +
                "\n- §a/icl forceclean§r: Immediately clears all items on the ground." +
                "\n- §a/icl reload§r: Reloads the ICL." +
                "\n- §a/icl config set <key> <value>§r: Changes a configuration value. To see current configuration value, use /icl config set <key>." +
                "\n- §a/icl cancel [seconds]§r: Cancels the next clear. If a number of seconds is provided, the next clear will be scheduled after that many seconds." +
                "\n" +
                "\n§l§nConfiguration§r\n" +
                "\nConfiguration values can be changed using the §a/icl config set§r command. Here are some of the configurable values:" +
                "\n" +
                "\n- §aDelay§r: The delay (in seconds) between automatic item clears." +
                "\n- §aNotificationDelay§r: The delay (in seconds) before a clear when a notification will be sent." +
                "\n- §aNotificationStart§r: The time (in seconds) when notifications start being sent before a clear." +
                "\n- §aNotificationTimes§r: The number of notifications to send before a clear." +
                "\n- §aCountdownStart§r: The time (in seconds) when the countdown starts before a clear." +
                "\n- §adoNotificationCountdown§r: Whether to show a countdown before a clear." +
                "\n- §adoNotificationSound§r: Whether to play a sound when a notification is sent." +
                "\n- §adoLastNotificationSound§r: Whether to play a sound when a last notification is sent." +
                "\n- §aNotificationSound§r: The sound to play when a notification is sent." +
                "\n- §aLastNotificationSound§r: The sound to play when a last notification is sent." +
                "\n- §aNotificationLang§r: The language for notifications." +
                "\n- §aNotificationColor§r: The color for notifications." +
                "\n- §aRequireOp§r: Whether to require OP to use ICL commands(except /icl, /icl cancel)." +
                "\n- §aRequireOpCancel§r: Whether to require OP to cancel a clear." +
                "\n- §apreserveNoDespawnItems§r: Whether to preserve items that are set to never despawn." +
                "\n- §apreserveNoPickupItems§r: Whether to preserve items that are set to never be picked up."

        );
        return translations;
    }

    private static Map<String, String> getDefaultRuRuTranslations() {
        Map<String, String> translations = new HashMap<>();
        translations.put("text.icl.notification", "Предметы на полу будут удалены через %d секунд");
        translations.put("text.icl.countdown", "Очистка предметов через %d");
        translations.put("text.icl.clear", "Очистка предметов");
        translations.put("text.icl.forceclear", "Принудительная очистка предметов");
        translations.put("text.icl.config.updated", "Значение конфига %s изменено на %s");
        translations.put("text.icl.reload", "ICL перезагружен");
        translations.put("text.icl.cancel.button", "[Отмена]");
        translations.put("text.icl.cancel.message", "Очистка предметов отменена");
        translations.put("text.icl.reload.fail", "ICL не удалось перезагрузить");
        translations.put("text.icl.clear.finish", "Очищено предметов: %d");
        translations.put("text.icl.config.current", "Текущее значение %s: %s (По умолчанию: %s)");
        translations.put("text.icl.readme", "§l§nКоманды§r\n" +
                "\nОсновная команда, предоставляемая ICL, - §a/icl§r, у которой есть несколько подкоманд:\n" +
                "\n- §a/icl forceclean§r: Немедленно очищает все предметы на полу." +
                "\n- §a/icl reload§r: Перезагружает ICL." +
                "\n- §a/icl config set <key> <value>§r: Изменяет значение конфигурации. Чтобы увидеть текущее значение конфигурации, используйте /icl config set <key>." +
                "\n- §a/icl cancel [seconds]§r: Отменяет следующую очистку. Если указано количество секунд, следующая очистка будет запланирована через столько секунд." +
                "\n" +
                "\n§l§nКонфигурация§r\n" +
                "\nЗначения конфигурации можно изменить с помощью команды §a/icl config set§r. Вот некоторые из настраиваемых значений:" +
                "\n" +
                "\n- §aDelay§r: Задержка (в секундах) между автоматическими очистками предметов." +
                "\n- §aNotificationDelay§r: Задержка (в секундах) перед очисткой, когда будет отправлено уведомление." +
                "\n- §aNotificationStart§r: Время (в секундах), когда начнут отправляться уведомления перед очисткой." +
                "\n- §aNotificationTimes§r: Количество уведомлений, отправляемых перед очисткой." +
                "\n- §aCountdownStart§r: Время (в секундах), когда начнется обратный отсчет перед очисткой." +
                "\n- §adoNotificationCountdown§r: Показывать ли обратный отсчет перед очисткой." +
                "\n- §adoNotificationSound§r: Воспроизводить ли звук при отправке уведомления." +
                "\n- §adoLastNotificationSound§r: Воспроизводить ли звук при отправке последнего уведомления." +
                "\n- §aNotificationSound§r: Звук, который воспроизводится при отправке уведомления." +
                "\n- §aLastNotificationSound§r: Звук, который воспроизводится при отправке последнего уведомления." +
                "\n- §aNotificationLang§r: Язык для уведомлений." +
                "\n- §aNotificationColor§r: Цвет для уведомлений." +
                "\n- §aRequireOp§r: Требуется ли OP для использования команд ICL (кроме /icl, /icl cancel)." +
                "\n- §aRequireOpCancel§r: Требуется ли OP для отмены очистки." +
                "\n- §apreserveNoDespawnItems§r: Следует ли сохранять предметы, которые никогда не исчезнут." +
                "\n- §apreserveNoPickupItems§r: Следует ли сохранять предметы, которые нельзя подобрать."
        );
        return translations;
    }
}