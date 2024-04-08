package vt.icl.config;

import java.lang.reflect.Type;
import java.util.HashMap;

public class Configuration {
    public long delay;
    public long notificationdelay;
    public long notificationstart;
    public int notificationtimes;
    public long countdownstart;
    public boolean notificationcountdown;
    public boolean showNotification;
    public boolean showNotificationSound;
    public String notificationSound;
    public String notificationLang;

    public Configuration() {
        this.delay = 80;
        this.notificationdelay = 15;
        this.notificationstart = 60;
        this.notificationtimes = 4;
        this.countdownstart = 5;
        this.notificationcountdown = true;
        this.showNotification = true;
        this.showNotificationSound = true;
        this.notificationSound = "minecraft:block.note_block.harp";
        this.notificationLang = "en_us";
    }

    public void save() {
        ConfigManager.setConfig(this);
    }

    // list of all the configuration field and its types example: delay/long
    public HashMap<String, Type> get() {
        HashMap<String, Type> map = new HashMap<>();
        map.put("delay", long.class);
        map.put("notificationdelay", long.class);
        map.put("notificationstart", long.class);
        map.put("notificationtimes", int.class);
        map.put("countdownstart", long.class);
        map.put("notificationcountdown", boolean.class);
        map.put("showNotification", boolean.class);
        map.put("showNotificationSound", boolean.class);
        map.put("notificationSound", String.class);
        map.put("notificationLang", String.class);
        return map;
    }

    public void set(Configuration configuration) {
        this.delay = configuration.delay;
        this.notificationdelay = configuration.notificationdelay;
        this.notificationstart = configuration.notificationstart;
        this.notificationtimes = configuration.notificationtimes;
        this.countdownstart = configuration.countdownstart;
        this.notificationcountdown = configuration.notificationcountdown;
        this.showNotification = configuration.showNotification;
        this.showNotificationSound = configuration.showNotificationSound;
        this.notificationSound = configuration.notificationSound;
        this.notificationLang = configuration.notificationLang;
    }

    public void set(String key, String value) {
        switch (key) {
            case "delay":
                this.delay = Long.parseLong(value);
                break;
            case "notificationdelay":
                this.notificationdelay = Long.parseLong(value);
                break;
            case "notificationstart":
                this.notificationstart = Long.parseLong(value);
                break;
            case "notificationtimes":
                this.notificationtimes = Integer.parseInt(value);
                break;
            case "countdownstart":
                this.countdownstart = Long.parseLong(value);
                break;
            case "notificationcountdown":
                this.notificationcountdown = Boolean.parseBoolean(value);
                break;
            case "showNotification":
                this.showNotification = Boolean.parseBoolean(value);
                break;
            case "showNotificationSound":
                this.showNotificationSound = Boolean.parseBoolean(value);
                break;
            case "notificationSound":
                this.notificationSound = value;
                break;
            case "notificationLang":
                this.notificationLang = value;
                break;
        }
        save();
    }


}
