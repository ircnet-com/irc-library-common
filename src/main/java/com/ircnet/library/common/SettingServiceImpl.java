package com.ircnet.library.common;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SettingServiceImpl implements SettingService {
    protected Map<String, Object> settings;

    public SettingServiceImpl() {
        this.settings = new HashMap<>();
    }

    @Override
    public void insert(String key, boolean value) {
        settings.put(key, value);
    }

    @Override
    public void insert(String key, int value) {
        settings.put(key, value);
    }

    @Override
    public boolean findBoolean(String key, boolean defaultValue) {
        Object value = settings.get(key);
        return value != null ? (boolean) value : defaultValue;
    }

    @Override
    public int findInteger(String key, int defaultValue) {
        Object value = settings.get(key);
        return value != null ? (int) value : defaultValue;
    }
}
