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
    protected Date nextConnectAttempt;
    protected String serverName;
    protected String sid;
    protected String version;
    protected boolean version212;
    protected ISupport iSupport;

    private boolean lagCheckInProgress;
    private int lag;
    private Date lagCheckSent;
    private Date lagCheckNext;
    private long lastProcessClientIteration;
    private long inboundLineSeq;
    private boolean aborted;
    private Map<String, Object> dynamicProperties;

    public IRCConnection() {
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.nextConnectAttempt = new Date();
        this.iSupport = new ISupport();
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

    public long nextInboundLineSeq() {
        return ++inboundLineSeq;
    }
}
