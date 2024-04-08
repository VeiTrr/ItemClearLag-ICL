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
            e.printStackTrace();
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
        translations.put("text.icl.reload.fail", "Failed to reload ICL");
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
        translations.put("text.icl.reload.fail", "ICL не удалось перезагрузить");
        return translations;
    }
}