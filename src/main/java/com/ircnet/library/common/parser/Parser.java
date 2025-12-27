package com.ircnet.library.common.parser;

import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.event.EventContext;

public interface Parser<T extends IRCConnection> {
    boolean parse(T ircConnection, String line, EventContext<T> eventContext);
}
