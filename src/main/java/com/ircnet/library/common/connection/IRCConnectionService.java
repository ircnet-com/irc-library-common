package com.ircnet.library.common.connection;

import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.configuration.IRCServerModel;

import java.io.IOException;

public interface IRCConnectionService {
    void connect(IRCConnection connection) throws IOException;

    void onConnectionEstablished(IRCConnection connection);

    void onDisconnect(IRCConnection connection, String message);

    void disconnect(IRCConnection connection);

    void quit(IRCConnection connection, String message);

    void onLineReceived(IRCConnection connection, String line);

    void send(IRCConnection connection, String text);

    void notice(IRCConnection connection, String target, String format, Object... args);

    void privmsg(IRCConnection connection, String target, String format, Object... args);

    IRCServerModel findRandomIRCServer(ConfigurationModel configurationModel);

    boolean processSSLInput(IRCConnection connection);

    void send(IRCConnection connection, String format, Object... args);

    ConfigurationModel getConfigurationModel();

    boolean isRegistered(IRCConnection connection);

    void reset(IRCConnection connection);

    String getIncompleteLine(IRCConnection connection);

    void setIncompleteLine(String incompleteLine);

    boolean shouldPerformLagCheck(IRCConnection connection);

    void checkLag(IRCConnection ircConnection);

    void handleLagCheckResponse(IRCConnection connection, String data);

    int getLag(IRCConnection connection);

    void resetLagCheck(IRCConnection connection);
}
