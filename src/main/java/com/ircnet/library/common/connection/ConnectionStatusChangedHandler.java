package com.ircnet.library.common.connection;

public interface ConnectionStatusChangedHandler {
    void onConnectionEstablished(IRCConnection ircConnection);
    void onRegistered(IRCConnection ircConnection);
    void onDisconnect(IRCConnection ircConnection, ConnectionStatus oldStatus);
}
