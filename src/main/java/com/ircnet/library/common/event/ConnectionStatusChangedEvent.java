package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.ConnectionStatus;
import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * - Connecting
 * - Connection established
 * - Registered (got 001 or 383)
 * - Disconnected
 */
@Getter
@SuperBuilder(toBuilder = true)
public class ConnectionStatusChangedEvent<T extends IRCConnection> extends AbstractEvent<T> {
    private ConnectionStatus oldStatus;
    private ConnectionStatus newStatus;
}
