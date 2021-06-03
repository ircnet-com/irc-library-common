package com.ircnet.library.common;

import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.connection.IRCConnection;

import java.util.HashMap;
import java.util.Map;

public abstract class IRCTask<S extends IRCConnection, T extends ConfigurationModel> {
    protected S ircConnection;

    protected T configuration;

    private boolean aborted;

    private long lastProcessClientIteration;

    private Map<String, Object> dynamicProperties;

    public IRCTask() {
        this.aborted = false;
        this.dynamicProperties = new HashMap<>();
    }

    public T getConfiguration() {
        return configuration;
    }

    public S getIRCConnection() {
        return ircConnection;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public long getLastProcessClientIteration() {
        return lastProcessClientIteration;
    }

    public void setLastProcessClientIteration(long lastProcessClientIteration) {
        this.lastProcessClientIteration = lastProcessClientIteration;
    }

    public Map<String, Object> getDynamicProperties() {
        return dynamicProperties;
    }

    public void setDynamicProperties(Map<String, Object> dynamicProperties) {
        this.dynamicProperties = dynamicProperties;
    }
}
