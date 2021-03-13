package com.ircnet.library.common;

public interface SettingService {
    void insert(String key, boolean value);

    void insert(String key, int value);

    boolean findBoolean(String key, boolean defaultValue);

    int findInteger(String key, int defaultValue);

    /**
     * Checks if a setting exists
     *
     * @param key The key of the setting
     * @return true if the setting exists, otherwise false
     */
    boolean exists(String key);
}
