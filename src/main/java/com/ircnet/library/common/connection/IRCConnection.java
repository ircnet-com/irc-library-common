package com.ircnet.library.common.connection;

import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.configuration.ServerModel;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class IRCConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCConnection.class);

    protected ConfigurationModel configurationModel;
    protected ServerModel currentServer;
    protected SocketChannel socketChannel;
    protected String incompleteLine;

    protected ConnectionStatus connectionStatus;
    protected Date connectTime;
    protected Date nexConnectAttempt;

    private boolean lagCheckInProgress;
    private int lag;
    private Date lagCheckSent;
    private Date lagCheckNext;

    private Map<String, Object> dynamicProperties;

    private boolean aborted;

    private long lastProcessClientIteration;

    public IRCConnection() {
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.nexConnectAttempt = new Date();
        this.dynamicProperties = new HashMap<>();
    }

    public IRCConnection(ConfigurationModel configurationModel) {
        this();
        this.configurationModel = configurationModel;
    }

    public ConfigurationModel getConfiguration() {
        return configurationModel;
    }

    public boolean isSSL() {
        return false;
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
}
