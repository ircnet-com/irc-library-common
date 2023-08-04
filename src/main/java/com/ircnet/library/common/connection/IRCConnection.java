package com.ircnet.library.common.connection;

import com.ircnet.library.common.configuration.ConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class IRCConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCConnection.class);

    protected ConfigurationModel configurationModel;
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

    public IRCConnection() {
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.nexConnectAttempt = new Date();
        this.dynamicProperties = new HashMap<>();
    }

    public IRCConnection(ConfigurationModel configurationModel) {
        this();
        this.configurationModel = configurationModel;
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }

    public void setConfigurationModel(ConfigurationModel configurationModel) {
        this.configurationModel = configurationModel;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public String getIncompleteLine() {
        return incompleteLine;
    }

    public void setIncompleteLine(String incompleteLine) {
        this.incompleteLine = incompleteLine;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Date getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(Date connectTime) {
        this.connectTime = connectTime;
    }

    public Date getNexConnectAttempt() {
        return nexConnectAttempt;
    }

    public void setNexConnectAttempt(Date nexConnectAttempt) {
        this.nexConnectAttempt = nexConnectAttempt;
    }

    public int getPenalty() {
        return 0;
    }

    public boolean isSSL() {
        return false;
    }

    public boolean isLagCheckInProgress() {
        return lagCheckInProgress;
    }

    public void setLagCheckInProgress(boolean lagCheckInProgress) {
        this.lagCheckInProgress = lagCheckInProgress;
    }

    public int getLag() {
        return lag;
    }

    public void setLag(int lag) {
        this.lag = lag;
    }

    public Date getLagCheckSent() {
        return lagCheckSent;
    }

    public void setLagCheckSent(Date lagCheckSent) {
        this.lagCheckSent = lagCheckSent;
    }

    public Date getLagCheckNext() {
        return lagCheckNext;
    }

    public void setLagCheckNext(Date lagCheckNext) {
        this.lagCheckNext = lagCheckNext;
    }

    public Map<String, Object> getDynamicProperties() {
        return dynamicProperties;
    }

    public void setDynamicProperties(Map<String, Object> dynamicProperties) {
        this.dynamicProperties = dynamicProperties;
    }
}
