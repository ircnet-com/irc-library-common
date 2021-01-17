package com.ircnet.library.common.connection;

import com.ircnet.library.common.Parser;
import com.ircnet.library.common.SettingConstants;
import com.ircnet.library.common.SettingService;
import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.configuration.IRCServerModel;
import com.ircnet.library.common.configuration.ServerModel;
import com.ircnet.library.common.event.ConnectionStatusChangedEvent;
import com.ircnet.library.common.event.EventBus;
import com.ircnet.library.common.event.ReceivedLineEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Random;

public class IRCConnectionServiceImpl implements IRCConnectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCConnectionServiceImpl.class);

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Parser parser;

    @Autowired
    private SettingService settingService;

    @Override
    public void connect(IRCConnection connection) throws IOException {
        ConnectionStatus oldConnectionStatus = connection.getConnectionStatus();
        ConfigurationModel configurationModel = connection.getConfigurationModel();

        ServerModel server;

        server = findRandomIRCServer(configurationModel);

        InetAddress inetAddress;

        try {
            inetAddress = Resolver.resolve(server);
        }
        catch (Exception e) {
            LOGGER.error("Failed to resolve {} protocol: {}", server.getHostname(), server.getProtocol());
            eventBus.publishEvent(new ConnectionStatusChangedEvent(connection, oldConnectionStatus, connection.getConnectionStatus()));
            return;
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, server.getPort());

        LOGGER.info("Connecting to {} ({}) port {}", server.getHostname(), inetSocketAddress.getAddress().getHostAddress(), server.getPort());

        connection.setSocketChannel(SocketChannel.open());
        connection.getSocketChannel().configureBlocking(false);

        if(!StringUtils.isBlank(configurationModel.getLocalAddress())) {
            connection.getSocketChannel().bind(new InetSocketAddress(configurationModel.getLocalAddress(), 0));
        }

        connection.setConnectionStatus(ConnectionStatus.CONNECTING);
        connection.setConnectTime(new Date());
        connection.getSocketChannel().connect(inetSocketAddress);

        eventBus.publishEvent(new ConnectionStatusChangedEvent(connection, oldConnectionStatus, connection.getConnectionStatus()));
    }

    @Override
    public void onConnectionEstablished(IRCConnection connection) {
        ConnectionStatus oldConnectionStatus = connection.getConnectionStatus();
        connection.setConnectionStatus(ConnectionStatus.CONNECTION_ESTABLISHED);

        if(!connection.isSSL()) {
            // Connection to IRC established
            eventBus.publishEvent(new ConnectionStatusChangedEvent(connection, oldConnectionStatus, connection.getConnectionStatus()));
        }
    }

    @Override
    public void onDisconnect(IRCConnection connection, String message) {
        ConnectionStatus oldConnectionStatus = connection.getConnectionStatus();
        connection.setConnectionStatus(ConnectionStatus.DISCONNECTED);
        connection.setConnectTime(null);

        eventBus.publishEvent(new ConnectionStatusChangedEvent(connection, oldConnectionStatus, connection.getConnectionStatus()));

        // TODO: Handle parse ERROR, maybe QUIT
    }

    @Override
    public void disconnect(IRCConnection connection) {
        try {
            connection.getSocketChannel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        onDisconnect(connection, "");
    }

    @Override
    public void quit(IRCConnection connection, String message) {
        send(connection, "QUIT :" + message);
    }

    @Override
    public void onLineReceived(IRCConnection connection, String line) {
        try {
            parser.parse(connection, line);
            eventBus.publishEvent(new ReceivedLineEvent(connection, line));
        }
        catch (Exception e) {
            LOGGER.error("Failed to parse '{}'", line, e);
        }
    }

    @Override
    public void send(IRCConnection connection, String text) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        //LOGGER.debug("[{}] {}", date, text);

        ByteBuffer bb = ByteBuffer.wrap((text+"\r\n").getBytes());

        try {
            connection.socketChannel.write(bb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notice(IRCConnection connection, String target, String format, Object... args) {
        String content = new Formatter().format(format, args).toString();
        send(connection, "NOTICE %s :%s", target, content);
    }

    @Override
    public void privmsg(IRCConnection connection, String target, String format, Object... args) {
        String content = new Formatter().format(format, args).toString();
        send(connection, "PRIVMSG %s :%s", target, content);
    }

    @Override
    public IRCServerModel findRandomIRCServer(ConfigurationModel configurationModel) {
        int index = new Random().nextInt(configurationModel.getIrcServers().size());
        return configurationModel.getIrcServers().get(index);
    }

    @Override
    public boolean processSSLInput(IRCConnection connection) {
        return false;
    }

    @Override
    public void send(IRCConnection connection, String format, Object... args) {
        send(connection, new Formatter().format(format, args).toString());
    }

    @Override
    public ConfigurationModel getConfigurationModel() {
        return null;
    }

    @Override
    public boolean isRegistered(IRCConnection connection) {
        return connection.getConnectionStatus() == ConnectionStatus.REGISTERED;
    }

    @Override
    public void reset(IRCConnection connection) {
    }

    @Override
    public String getIncompleteLine(IRCConnection connection) {
        return null;
    }

    @Override
    public void setIncompleteLine(String incompleteLine) {

    }

    @Override
    public boolean shouldPerformLagCheck(IRCConnection connection) {
        return !connection.isLagCheckInProgress() && connection.getPenalty() == 0 && connection.getLagCheckNext() != null && System.currentTimeMillis() >= connection.getLagCheckNext().getTime();
    }

    @Override
    public void checkLag(IRCConnection ircConnection) {
        Date now = new Date();
        send(ircConnection, "PING :%s", now.getTime());
        ircConnection.setLagCheckInProgress(true);
        ircConnection.setLagCheckSent(now);
        ircConnection.setLagCheckNext(null);
    }

    @Override
    public void handleLagCheckResponse(IRCConnection connection, String data) {
        if (connection.isLagCheckInProgress() && connection.getLagCheckSent().getTime() == Long.valueOf(data)) {
            connection.setLag((int) ((System.currentTimeMillis() - connection.getLagCheckSent().getTime()) / 1000));
            LOGGER.debug("Current lag is {} seconds", connection.getLag());
            connection.setLagCheckInProgress(false);
        }

        connection.setLagCheckNext(new Date(System.currentTimeMillis() + settingService.findInteger(SettingConstants.LAGCHECK_INTERVAL, SettingConstants.LAGCHECK_INTERVAL_DEFAULT) * 1000));
    }

    @Override
    public int getLag(IRCConnection connection) {
        if (connection.isLagCheckInProgress()) {
            connection.setLag((int) ((System.currentTimeMillis() - connection.getLagCheckSent().getTime()) / 1000));
        }

        return connection.getLag();
    }

    @Override
    public void resetLagCheck(IRCConnection connection) {
        connection.setLagCheckInProgress(false);
        connection.setLag(0);
        connection.setLagCheckSent(null);
        connection.setLagCheckNext(null);
    }
}
