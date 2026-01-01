package com.ircnet.library.common.connection;

public interface ConnectionStatusChangedHandler {
    void onConnecting(IRCConnection ircConnection);
    void onConnectionEstablished(IRCConnection ircConnection);
    void onRegistered(IRCConnection ircConnection);
    void onConnectFailed(IRCConnection ircConnection);
    void onDisconnect(IRCConnection ircConnection, ConnectionStatus oldStatus);
}
