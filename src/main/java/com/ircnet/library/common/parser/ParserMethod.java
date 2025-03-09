package com.ircnet.library.common.parser;

import com.ircnet.library.common.connection.IRCConnection;

import java.util.Map;

@FunctionalInterface
public interface ParserMethod<T extends IRCConnection> {
    void parse(T ircConnection, String[] parts, Map<String, String> messageTags);
}
