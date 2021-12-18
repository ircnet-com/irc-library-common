package com.ircnet.library.parser;

import com.ircnet.library.common.connection.IRCConnection;

@FunctionalInterface
public interface ParserMethod<T extends IRCConnection> {
    void parse(T ircConnection, String[] parts);
}