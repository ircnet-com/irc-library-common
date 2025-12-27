package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;

import java.util.Objects;

public final class EventContext<T extends IRCConnection> {
    private final T ircConnection;
    private final Long lineSeq;

    public EventContext(T ircConnection, Long lineSeq) {
        this.ircConnection = Objects.requireNonNull(ircConnection);
        this.lineSeq = lineSeq;
    }

    public T getIrcConnection() {
        return ircConnection;
    }

    public Long getLineSeq() {
        return lineSeq;
    }
}

