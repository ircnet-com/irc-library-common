package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
public class ReceivedLineEvent<T extends IRCConnection> extends AbstractEvent<T> {
}
