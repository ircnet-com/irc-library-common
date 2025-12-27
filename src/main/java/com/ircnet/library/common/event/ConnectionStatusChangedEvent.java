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

    public ConnectionStatusChangedEvent(EventContext context, ConnectionStatus oldStatus, ConnectionStatus newStatus) {
        super(context);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
