package vt.icl.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import vt.icl.ICLCommon;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = ICLCommon.MOD_ID.toUpperCase() + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Configuration config;

    public static void loadConfig() {
        File configFile = ICLCommon.CONFIG_DIR.resolve(CONFIG_FILE_NAME).toFile();
        if (configFile.exists()) {
            try {
                JsonObject jsonObject = JsonParser.parseString(FileUtils.readFileToString(configFile, "UTF-8")).getAsJsonObject();
                config = GSON.fromJson(jsonObject, Configuration.class);
                if (config == null) {
                    config = new Configuration();
                }
                mergeMissingFields(jsonObject, config, new Configuration());
            } catch (Exception e) {
                ICLCommon.LOGGER.info("Failed to load config file " + e.getMessage());
                config = new Configuration();
            }
        } else {
            config = new Configuration();
        }
        saveConfig();
    }

    public static Configuration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void setConfig(Configuration config) {
        ConfigManager.config = config;
        saveConfig();
    }

    public static void saveConfig() {
        File configFile = ICLCommon.CONFIG_DIR.resolve(CONFIG_FILE_NAME).toFile();
        try {
            FileUtils.forceMkdirParent(configFile);
            FileUtils.writeStringToFile(configFile, GSON.toJson(config), "UTF-8");
        } catch (Exception e) {
            ICLCommon.LOGGER.info("Failed to save config file " + e.getMessage());
        }
    }

    private static void mergeMissingFields(JsonObject jsonObject, Configuration config, Configuration defaults) throws IllegalAccessException {
        for (Field field : Configuration.class.getFields()) {
            // getFields() also returns the public constants declared here, and a
            // static final field cannot be assigned. Without this guard, adding
            // any constant to Configuration makes every load throw and silently
            // reset the whole file to its defaults.
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String key = field.getName();
            if (!jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
                field.set(config, field.get(defaults));
            }
        }
    }
}
