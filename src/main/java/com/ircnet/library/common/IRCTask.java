package com.ircnet.library.common;

import com.ircnet.library.common.connection.ConnectionStatus;
import com.ircnet.library.common.connection.IRCConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public abstract class IRCTask<S extends IRCConnection, T extends Client> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCTask.class);

    private List<T> clientList = new ArrayList<>();

    protected Parser parser;

    private static SettingService settingService;

    public IRCTask() {
        settingService = new SettingServiceImpl();
    }

    public List<T> getClientList() {
        return clientList;
    }

    public void add(T client) {
        clientList.add(client);
    }

    public void removeByUserId(String userId) {
        for(T client : getClientList()) {
            if(client.getConfiguration().getUserId().equals(userId)) {
                getClientList().remove(client);
                return;
            }
        }
    }

    public void run()  {
        registerEventListeners();

        long lastTimeMillis = System.currentTimeMillis();

        Selector selector;

        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("Failed to open selector", e);
            return;
        }

        while (true) {
            try {
                long now = System.currentTimeMillis();

                if (selector.select(300) > 0) {
                    processReadySet(selector.selectedKeys());
                }

                if (now - lastTimeMillis >= 1000) {
                    lastTimeMillis = System.currentTimeMillis();

                    afterOneSecond();
                }

                Iterator<T> clientIterator = getClientList().iterator();

                while (clientIterator.hasNext()) {
                    T client = clientIterator.next();

                    processClientIteration(client, selector);
                }
            }
            catch (Exception e) {
                LOGGER.error("An error occurred", e);
            }
        }
    }

    protected void processClientIteration(T client, Selector selector) throws IOException {
        S ircConnection = (S) client.getIRCConnection();

        long now = System.currentTimeMillis();

        // Connect
        if (client.getConfiguration().isAutoConnectEnabled() && ircConnection.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            if (new Date().getTime() >= ircConnection.getNexConnectAttempt().getTime()) {
                ircConnection.connect();

                if (ircConnection.getSocketChannel() != null && ircConnection.getSocketChannel().isOpen())
                    ircConnection.getSocketChannel().register(selector, SelectionKey.OP_CONNECT);
            }
        }

        // Timeout for connect and registration
        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTING) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.CONNECT_TIMEOUT, SettingConstants.CONNECT_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Connect timed out");
                ircConnection.disconnect();
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTION_ESTABLISHED) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.REGISTRATION_TIMEOUT, SettingConstants.REGISTRATION_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Registration timeout - disconnecting");
                ircConnection.disconnect();
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.REGISTERED) {
            int maxLagBeforeDisconnect = settingService.findInteger(SettingConstants.MAX_LAG_BEFORE_DISCONNECT, SettingConstants.MAX_LAG_BEFORE_DISCONNECT_DEFAULT);

            if (maxLagBeforeDisconnect != 0 && ircConnection.getLagCheck().getLag() >= maxLagBeforeDisconnect) {
                LOGGER.info("Disconnecting after lag of {} seconds", maxLagBeforeDisconnect);
                ircConnection.getLagCheck().reset();
                ircConnection.disconnect();
                return;
            }
        }
    }

    protected void afterOneSecond() {
    }

    protected abstract void registerEventListeners();

    private S findIRCConnectionForSocketChannel(SocketChannel socketChannel) {
        for (T client : getClientList())
            if (client.getIRCConnection().getSocketChannel() == socketChannel)
                return (S) client.getIRCConnection();

        return null;
    }

    protected void processReadySet(Set readySet) {
        Iterator iterator = readySet.iterator();

        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();

            SocketChannel sc = (SocketChannel) key.channel();
            S ircConnection = findIRCConnectionForSocketChannel(sc);

            if (ircConnection == null) {
                LOGGER.debug("Failed to find irc connection for socket channel {}", sc);
                key.cancel();
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                iterator.remove();
                continue;
            }

            if (ircConnection.isSSL()) {
                if(ircConnection.processSSLInput()) {
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
                        ircConnection.onDisconnect(e.getMessage());
                        continue;
                    }

                    if (ircConnection.isSSL())
                        setupSSL(key, ircConnection);

                    ircConnection.onConnectionEstablished();
                    key.interestOps(SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    ByteBuffer bb = ByteBuffer.allocate(1024);

                    try {
                        if (sc.read(bb) == -1) {
                            key.cancel();
                            sc.close();
                            ircConnection.onDisconnect(null);
                            continue;
                        }
                    } catch (Exception e) {
                        key.cancel();
                        try {
                            sc.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        ircConnection.onDisconnect(null);
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

    protected void processInput(S ircConnection, ByteBuffer bb) {
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
                ircConnection.onLineReceived(lines[i]);
            }
        }

        if (hasRest)
            ircConnection.setIncompleteLine(lines[lines.length - 1]);
    }

    protected void setupSSL(SelectionKey key, S ircConnection) {
    }

    public Parser getParser() {
        return parser;
    }

    public static SettingService getSettingService() {
        return settingService;
    }

    public static void setSettingService(SettingService settingService) {
        IRCTask.settingService = settingService;
    }
}
