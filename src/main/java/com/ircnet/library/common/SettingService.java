package com.ircnet.library.common;

public interface SettingService {
    void insert(String key, boolean value);

    void insert(String key, int value);

    boolean findBoolean(String key, boolean defaultValue);

    int findInteger(String key, int defaultValue);
}
