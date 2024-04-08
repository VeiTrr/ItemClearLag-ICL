package vt.icl.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "Icl/ICL.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Configuration config;

    public static void loadConfig() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME).toFile();
        if (configFile.exists()) {
            try {
                config = GSON.fromJson(FileUtils.readFileToString(configFile, "UTF-8"), Configuration.class);
            } catch (Exception e) {
                e.printStackTrace();
                config = new Configuration();
                saveConfig();
            }
        } else {
            config = new Configuration();
            saveConfig();
        }
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
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME).toFile();
        try {
            FileUtils.writeStringToFile(configFile, GSON.toJson(config), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
