package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.ConnectionStatus;
import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;

/**
 * - Connecting
 * - Connection established
 * - Registered (got 001 or 383)
 * - Disconnected
 */
@Getter
public class ConnectionStatusChangedEvent extends AbstractEvent {
    private ConnectionStatus oldStatus;
    private ConnectionStatus newStatus;

    public ConnectionStatusChangedEvent(IRCConnection ircConnection, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
        this.ircConnection = ircConnection;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
