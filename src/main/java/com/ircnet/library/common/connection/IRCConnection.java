package com.ircnet.library.common.connection;

import com.ircnet.library.common.IRCTask;
import com.ircnet.library.common.Util;
import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.configuration.ServerModel;
import com.ircnet.library.common.event.ConnectionStatusChangedEvent;
import com.ircnet.library.common.event.EventBus;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;

import com.ircnet.library.common.event.ReceivedLineEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IRCConnection<S extends IRCTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCConnection.class);

    protected S ircTask;
    protected ConfigurationModel configurationModel;
    protected SocketChannel socketChannel;
    protected String incompleteLine;

    protected ConnectionStatus connectionStatus;
    protected Date connectTime;
    protected Date nexConnectAttempt;

    protected LagCheck lagCheck;

    protected EventBus eventBus;

    public IRCConnection(S ircTask, ConfigurationModel configurationModel) {
        this.ircTask = ircTask;
        this.configurationModel = configurationModel;
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.nexConnectAttempt = new Date();
        this.lagCheck = new LagCheck();
        this.eventBus = ircTask.getEventBus();
    }

    public void connect() throws IOException {
        ConnectionStatus oldConnectionStatus = this.connectionStatus;

        ServerModel server;

        server = Util.findRandomIRCServer(configurationModel);

        InetAddress inetAddress;

        try {
            inetAddress = Resolver.resolve(server);
        }
        catch (Exception e) {
            LOGGER.error("Failed to resolve {} protocol: {}", server.getHostname(), server.getProtocol());
            eventBus.publishEvent(new ConnectionStatusChangedEvent(this, oldConnectionStatus, this.connectionStatus));
            return;
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, server.getPort());

        LOGGER.info("Connecting to {} ({}) port {}", server.getHostname(), inetSocketAddress.getAddress().getHostAddress(), server.getPort());

        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(false);

        if(!StringUtils.isBlank(configurationModel.getLocalAddress()))
            this.socketChannel.bind(new InetSocketAddress(configurationModel.getLocalAddress(), 0));

        this.connectionStatus = ConnectionStatus.CONNECTING;
        this.connectTime = new Date();
        this.socketChannel.connect(inetSocketAddress);

        eventBus.publishEvent(new ConnectionStatusChangedEvent(this, oldConnectionStatus, this.connectionStatus));
    }

    public void onConnectionEstablished() {
        ConnectionStatus oldConnectionStatus = this.connectionStatus;
        this.connectionStatus = ConnectionStatus.CONNECTION_ESTABLISHED;

        if(!isSSL()) {
            // Connection to IRC established
            eventBus.publishEvent(new ConnectionStatusChangedEvent(this, oldConnectionStatus, this.connectionStatus));
        }
    }

    public void onDisconnect(String message) {
        ConnectionStatus oldConnectionStatus = this.connectionStatus;
        this.connectionStatus = ConnectionStatus.DISCONNECTED;
        this.connectTime = null;

        eventBus.publishEvent(new ConnectionStatusChangedEvent(this, oldConnectionStatus, this.connectionStatus));

        // TODO: Handle parse ERROR, maybe QUIT
    }

    public void disconnect() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        onDisconnect("");
    }

    public void quit(String message) {
        send("QUIT :" + message);
    }

    public void onLineReceived(String line) {
        try {
            ircTask.getParser().parse(this, line);
            eventBus.publishEvent(new ReceivedLineEvent(this, line));
        }
        catch (Exception e) {
            LOGGER.error("Failed to parse '{}'", line, e);
        }
    }

    public void send(String text) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        //LOGGER.debug("[{}] {}", date, text);

        ByteBuffer bb = ByteBuffer.wrap((text+"\r\n").getBytes());

        try {
            this.socketChannel.write(bb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notice(String target, String format, Object... args) {
        String content = new Formatter().format(format, args).toString();
        send("NOTICE %s :%s", target, content);
    }

    public void privmsg(String target, String format, Object... args) {
        String content = new Formatter().format(format, args).toString();
        send("PRIVMSG %s :%s", target, content);
    }

    public boolean processSSLInput() {
        return false;
    }

    public void send(String format, Object... args) {
        send(new Formatter().format(format, args).toString());
    }

    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }

    public boolean isRegistered() {
        return getConnectionStatus() == ConnectionStatus.REGISTERED;
    }

    public void reset() {
    }

    public String getIncompleteLine() {
        return incompleteLine;
    }

    public void setIncompleteLine(String incompleteLine) {
        this.incompleteLine = incompleteLine;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
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

    public LagCheck getLagCheck() {
        return lagCheck;
    }

    public Date getConnectTime() {
        return connectTime;
    }

    public boolean isSSL() {
        return false;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
