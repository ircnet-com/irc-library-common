package com.ircnet.library.common;

import com.ircnet.library.common.connection.ConnectionStatus;
import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class IRCTaskServiceImpl implements IRCTaskService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IRCTask.class);

    @Autowired
    private IRCConnectionService ircConnectionService;

    @Autowired
    private SettingService settingService;

    @Override
    public void run(IRCTask ircTask)  {
        run(new ArrayList<>(Arrays.asList(ircTask)));
    }

    @Override
    public void run(List<? extends IRCTask> ircTasks)  {
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

            Iterator<? extends IRCTask> iterator = ircTasks.iterator();

            while(iterator.hasNext()) {
                IRCTask ircTask = iterator.next();

                if(ircTask.isAborted()) {
                    LOGGER.info("Terminating task for {}", ircTask.getConfiguration().getUserId());
                    iterator.remove();
                    continue;
                }

                try {
                    if (now - lastTimeMillis >= 1000) {
                        afterOneSecond(ircTask);
                    }

                    processClientIteration(ircTask, selector);
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

    protected void processClientIteration(IRCTask ircTask, Selector selector) throws IOException {
        IRCConnection ircConnection = ircTask.getIRCConnection();
        long now = System.currentTimeMillis();
        ircTask.setLastProcessClientIteration(now);

        // Connect
        if (!ircTask.isAborted() && ircTask.getConfiguration().isAutoConnectEnabled() && ircConnection.getConnectionStatus() == ConnectionStatus.DISCONNECTED) {
            if (new Date().getTime() >= ircConnection.getNexConnectAttempt().getTime()) {
                ircConnectionService.connect(ircConnection);

                if (ircConnection.getSocketChannel() != null && ircConnection.getSocketChannel().isOpen()) {
                    SelectionKey selectionKey = ircConnection.getSocketChannel().register(selector, SelectionKey.OP_CONNECT);
                    selectionKey.attach(ircTask);
                }
            }
        }

        // Timeout for connect and registration
        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTING) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.CONNECT_TIMEOUT, SettingConstants.CONNECT_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Connect timed out");
                ircConnectionService.disconnect(ircConnection);
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.CONNECTION_ESTABLISHED) {
            if (now - ircConnection.getConnectTime().getTime() >= settingService.findInteger(SettingConstants.REGISTRATION_TIMEOUT, SettingConstants.REGISTRATION_TIMEOUT_DEFAULT) * 1000) {
                LOGGER.info("Registration timeout - disconnecting");
                ircConnectionService.disconnect(ircConnection);
            }

            return;
        }

        if (ircConnection.getConnectionStatus() == ConnectionStatus.REGISTERED) {
            int maxLagBeforeDisconnect = settingService.findInteger(SettingConstants.MAX_LAG_BEFORE_DISCONNECT, SettingConstants.MAX_LAG_BEFORE_DISCONNECT_DEFAULT);

            if (maxLagBeforeDisconnect != 0 && ircConnectionService.getLag(ircConnection) >= maxLagBeforeDisconnect) {
                LOGGER.info("Disconnecting after lag of {} seconds", maxLagBeforeDisconnect);
                ircConnectionService.resetLagCheck(ircConnection);
                ircConnectionService.disconnect(ircConnection);
                return;
            }
        }

        sendQueuedMessages(ircConnection);

        if (ircConnectionService.shouldPerformLagCheck(ircConnection)) {
            ircConnectionService.checkLag(ircConnection);
        }
    }

    protected void afterOneSecond(IRCTask ircTask) {
    }

    protected void sendQueuedMessages(IRCConnection connection) {
    }

    protected void processReadySet(Set readySet) {
        Iterator iterator = readySet.iterator();

        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            SocketChannel sc = (SocketChannel) key.channel();
            IRCTask ircTask = (IRCTask) key.attachment();
            IRCConnection ircConnection = ircTask.getIRCConnection();

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
                if(ircConnectionService.processSSLInput(ircConnection)) {
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
                        ircConnectionService.onDisconnect(ircConnection, e.getMessage());
                        continue;
                    }

                    if (ircConnection.isSSL())
                        setupSSL(ircTask, key);

                    ircConnectionService.onConnectionEstablished(ircConnection);
                    key.interestOps(SelectionKey.OP_READ);
                }

                if (key.isReadable()) {
                    ByteBuffer bb = ByteBuffer.allocate(1024);

                    try {
                        if (sc.read(bb) == -1) {
                            key.cancel();
                            sc.close();
                            ircConnectionService.onDisconnect(ircConnection, null);
                            continue;
                        }
                    } catch (Exception e) {
                        key.cancel();
                        try {
                            sc.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        ircConnectionService.onDisconnect(ircConnection, null);
                        continue;
                    }

                    processInput(ircTask, bb);
                }
            } catch (CancelledKeyException exception) {
                // Ignore
                exception.printStackTrace();
            }

            iterator.remove();
        }
    }

    protected void processInput(IRCTask ircTask, ByteBuffer bb) {
        IRCConnection ircConnection = ircTask.getIRCConnection();

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
                ircConnectionService.onLineReceived(ircConnection, lines[i]);
            }
        }

        if (hasRest)
            ircConnection.setIncompleteLine(lines[lines.length - 1]);
    }

    protected void setupSSL(IRCTask ircTask, SelectionKey key) {
    }
}
