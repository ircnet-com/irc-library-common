package com.ircnet.library.common;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

public class SettingServiceImpl implements SettingService {
    protected Map<String, Object> settings;
    protected int defaultLagcheckInterval;
    protected int defaultMaxLagBeforeDisconnect;

    public SettingServiceImpl(int defaultLagcheckInterval, int defaultMaxLagBeforeDisconnect) {
        this.settings = new HashMap<>();
        this.defaultLagcheckInterval = defaultLagcheckInterval;
        this.defaultMaxLagBeforeDisconnect = defaultMaxLagBeforeDisconnect;
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

    @Override
    public boolean exists(String key) {
        return settings.containsKey(key);
    }

    @PostConstruct
    protected void init() {
        if(!exists(SettingConstants.LAGCHECK_INTERVAL) && defaultLagcheckInterval != 0) {
            insert(SettingConstants.LAGCHECK_INTERVAL, defaultLagcheckInterval);
        }

        if(!exists(SettingConstants.MAX_LAG_BEFORE_DISCONNECT) && defaultMaxLagBeforeDisconnect != 0) {
            insert(SettingConstants.MAX_LAG_BEFORE_DISCONNECT, defaultMaxLagBeforeDisconnect);
        }
    }
}
