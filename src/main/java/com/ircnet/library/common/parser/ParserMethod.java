package com.ircnet.library.common.parser;

import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.event.EventContext;

import java.util.Map;

@FunctionalInterface
public interface ParserMethod<T extends IRCConnection> {
    void parse(T ircConnection, String[] parts, Map<String, String> messageTags, EventContext<T> lineSeq, String line);
}
