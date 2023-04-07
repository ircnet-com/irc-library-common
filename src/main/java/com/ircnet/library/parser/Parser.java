package com.ircnet.library.parser;

import com.ircnet.library.common.connection.IRCConnection;

public interface Parser<T extends IRCConnection> {
    boolean parse(T ircConnection, String line);
}
