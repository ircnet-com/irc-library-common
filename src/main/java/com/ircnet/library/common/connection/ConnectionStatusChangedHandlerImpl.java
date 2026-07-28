package com.ircnet.library.common.connection;

public class ConnectionStatusChangedHandlerImpl implements ConnectionStatusChangedHandler {
    @Override
    public void onConnecting(IRCConnection ircConnection) {
    }

    @Override
    public void onConnectionEstablished(IRCConnection ircConnection) {
    }

    @Override
    public void onRegistered(IRCConnection ircConnection) {
        ircConnection.setISupport(new ISupport());
    }

    @Override
    public void onConnectFailed(IRCConnection ircConnection) {
    }

    @Override
    public void onDisconnect(IRCConnection ircConnection, ConnectionStatus oldStatus) {
    }
}
