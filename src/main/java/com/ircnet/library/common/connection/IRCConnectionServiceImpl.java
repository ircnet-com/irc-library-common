package com.ircnet.library.common.connection;

import com.ircnet.library.common.SettingConstants;
import com.ircnet.library.common.SettingService;
import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.configuration.IRCServerModel;
import com.ircnet.library.common.event.ConnectionStatusChangedEvent;
import com.ircnet.library.common.event.EventBus;
import com.ircnet.library.common.event.ReceivedLineEvent;
import com.ircnet.library.common.parser.Parser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public abstract class IRCConnectionServiceImpl implements IRCConnectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCConnectionServiceImpl.class);

    protected EventBus eventBus;
    protected Parser parser;
    protected SettingService settingService;
    protected ResolveService resolveService;

    public IRCConnectionServiceImpl(EventBus eventBus,
                                    Parser parser,
                                    SettingService settingService,
                                    ResolveService resolveService) {
        this.eventBus = eventBus;
        this.parser = parser;
        this.settingService = settingService;
        this.resolveService = resolveService;
    }

    @Override
    public void run(IRCConnection ircTask)  {
        run(new ArrayList<>(Arrays.asList(ircTask)));
    }

    @Override
    public void run(List<? extends IRCConnection> ircTasks)  {
        long lastTimeMillis = System.currentTimeMillis();

        Selector selector;

        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("Failed to open selector", e);
            return;
        }

        while(true) {
            Instant iterationBegin = Instant.now();
            long now = System.currentTimeMillis();

            try {
                if (selector.select(300) > 0) {
                    processReadySet(selector.selectedKeys());
                }
            }
            catch (Exception e) {
                LOGGER.error("An error occurred", e);
            }

            Iterator<? extends IRCConnection> iterator = ircTasks.iterator();

            while(iterator.hasNext()) {
                IRCConnection ircConnection = iterator.next();

                if(ircConnection.isAborted()) {
                    LOGGER.info("Terminating task for {}", ircConnection.getConfiguration().getUserId());
                    iterator.remove();
                    continue;
                }

                try {
                    if (now - lastTimeMillis >= 1000) {
                        afterOneSecond(ircConnection);
                    }

                    processClientIteration(ircConnection, selector);
                }
                catch (Exception e) {
                    LOGGER.error("An error occurred", e);
                }
            }

            if (now - lastTimeMillis >= 1000) {
                lastTimeMillis = System.currentTimeMillis();
            }

            Instant iterationEnd = Instant.now();
            long secondsElapsed = Duration.between(iterationBegin, iterationEnd).toMillis() / 1000;

            if(secondsElapsed >= 5) {
                LOGGER.warn("Iteration took {} seconds", secondsElapsed);
            }
        }
    }

    protected void processClientIteration(IRCConnection ircConnection, Selector selector) throws IOException {
        long now = System.currentTimeMillis();
        ircConnection.setLastProcessClientIteration(now);

        // Connect
        if (!ircConnection.isAborted() && ircConnection.getConfiguration().isAutoConnectEnabled() && ircConnection.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            if (new Date().getTime() >= ircConnection.getNexConnectAttempt().getTime()) {
                connect(ircConnection);

                if (ircConnection.getSocketChannel() != null && ircConnection.getSocketChannel().isOpen()) {
                    SelectionKey selectionKey = ircConnection.getSocketChannel().register(selector, SelectionKey.OP_CONNECT);
                    selectionKey.attach(ircConnection);
                }
            }
        }

        // Timeout for connect and registration
        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTING) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.CONNECT_TIMEOUT, SettingConstants.CONNECT_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Connect timed out");
                disconnect(ircConnection);
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTION_ESTABLISHED) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.REGISTRATION_TIMEOUT, SettingConstants.REGISTRATION_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Registration timeout - disconnecting");
                disconnect(ircConnection);
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.REGISTERED) {
            int maxLagBeforeDisconnect = settingService.findInteger(SettingConstants.MAX_LAG_BEFORE_DISCONNECT, SettingConstants.MAX_LAG_BEFORE_DISCONNECT_DEFAULT);

            if (maxLagBeforeDisconnect != 0 && getLag(ircConnection) >= maxLagBeforeDisconnect) {
                LOGGER.info("Disconnecting after lag of {} seconds", maxLagBeforeDisconnect);
                resetLagCheck(ircConnection);
                disconnect(ircConnection);
                return;
            }
        }

        sendQueuedMessages(ircConnection);

        if (shouldPerformLagCheck(ircConnection)) {
            checkLag(ircConnection);
        }
    }

    protected void afterOneSecond(IRCConnection ircTask) {
    }

    protected void sendQueuedMessages(IRCConnection connection) {
    }

    protected void processReadySet(Set readySet) {
        Iterator iterator = readySet.iterator();

        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            SocketChannel sc = (SocketChannel) key.channel();
            IRCConnection ircConnection = (IRCConnection) key.attachment();

            if (ircConnection == null) {
                LOGGER.debug("Failed to find irc connection for socket channel {}", sc);
                key.cancel();
                try {
                    sc.close();
                } catch (IOException e) {
                    LOGGER.debug("Could not close socket", e);
                }
                iterator.remove();
                continue;
            }

            if (ircConnection.isSSL()) {
                if(processSSLInput(ircConnection)) {
                    iterator.remove();
                    continue;
                }
            }

            try {
                if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTING && key.isConnectable()) {
                    try {
                        if (sc.isConnectionPending()) {
                            sc.finishConnect();
                        }
                    } catch (IOException e) {
                        key.cancel();
                        try {
                            sc.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        onDisconnect(ircConnection, e.getMessage());
                        continue;
                    }

                    if (ircConnection.isSSL())
                        setupSSL(ircConnection, key);

                    onConnectionEstablished(ircConnection);
                    key.interestOps(SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    ByteBuffer bb = ByteBuffer.allocate(1024);

                    try {
                        if (sc.read(bb) == -1) {
                            key.cancel();
                            sc.close();
                            onDisconnect(ircConnection, null);
                            continue;
                        }
                    } catch (Exception e) {
                        key.cancel();
                        try {
                            sc.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        onDisconnect(ircConnection, null);
                        continue;
                    }

                    processInput(ircConnection, bb);
                }
            } catch (CancelledKeyException exception) {
                // Ignore
                exception.printStackTrace();
            }

            iterator.remove();
        }
    }

    protected void processInput(IRCConnection ircConnection, ByteBuffer bb) {
        //IRCConnection ircConnection = ircTask.getIRCConnection();

        String input = new String(bb.array());
        input = input.replace("\u0000", "");

        String parsableInput;

        if(!StringUtils.isEmpty(ircConnection.getIncompleteLine())) {
            parsableInput = ircConnection.getIncompleteLine() + input;
            ircConnection.setIncompleteLine("");
        }
        else {
            parsableInput = input;
        }

        String[] lines = parsableInput.split("\r\n");

        boolean hasRest = !parsableInput.endsWith("\r\n");

        for (int i = 0; i < lines.length; i++) {
            if (i < lines.length - 1 || !hasRest) {
                onLineReceived(ircConnection, lines[i]);
            }
        }

        if (hasRest)
            ircConnection.setIncompleteLine(lines[lines.length - 1]);
    }

    protected void setupSSL(IRCConnection ircConnection, SelectionKey key) {
    }

    @Override
    public void connect(IRCConnection connection) throws IOException {
        ConnectionStatus oldConnectionStatus = connection.getConnectionStatus();
        ConfigurationModel configurationModel = connection.getConfigurationModel();

        IRCServerModel server = findRandomIRCServer(configurationModel);
        connection.setCurrentServer(server);

        InetAddress inetAddress;

        try {
            inetAddress = resolveService.resolve(server);
        }
        catch (Exception e) {
            LOGGER.error("Failed to resolve {} protocol: {}", server.getAddress(), server.getProtocol());
            eventBus.publishEvent(new ConnectionStatusChangedEvent(connection, oldConnectionStatus, connection.getConnectionStatus()));
            return;
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, server.getPort());

        LOGGER.info("Connecting to {} ({}) port {}", server.getAddress(), inetSocketAddress.getAddress().getHostAddress(), server.getPort());

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
        ByteBuffer bb = ByteBuffer.wrap((text+"\r\n").getBytes());

        try {
            connection.socketChannel.write(bb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notice(IRCConnection connection, String target, String format, Object... args) {
        String content;

        if(!ArrayUtils.isEmpty(args)) {
            content = new Formatter().format(format, args).toString();
        }
        else {
            content = format;
        }

        send(connection, "NOTICE %s :%s", target, content);
    }

    @Override
    public void privmsg(IRCConnection connection, String target, String format, Object... args) {
        String content;

        if(!ArrayUtils.isEmpty(args)) {
            content = new Formatter().format(format, args).toString();
        }
        else {
            content = format;
        }

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
        return connection.getConnectionStatus() == ConnectionStatus.REGISTERED
            && !connection.isLagCheckInProgress()
            && connection.getLagCheckNext() != null
            && System.currentTimeMillis() >= connection.getLagCheckNext().getTime();
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
