package com.ircnet.library.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class SettingServiceImpl implements SettingService {
    @Value("${lagcheck.interval:" + SettingConstants.LAGCHECK_INTERVAL_DEFAULT + "}")
    private int defaultLagcheckInterval;

    @Value("${lagcheck.max-lag-before-disconnect:" + SettingConstants.MAX_LAG_BEFORE_DISCONNECT_DEFAULT + "}")
    private int defaultMaxLagBeforeDisconnect;

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
