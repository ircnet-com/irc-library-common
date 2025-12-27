package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;

import java.util.Objects;

public abstract class AbstractEvent<T extends IRCConnection> {
    private final EventContext<T> context;

    protected AbstractEvent(EventContext<T> context) {
        this.context = Objects.requireNonNull(context);
    }

    public EventContext<T> getContext() {
        return context;
    }

    public T getIRCConnection() {
        return context.getIrcConnection();
    }

    public long getLineSeq() {
        return context.getLineSeq();
    }
}
