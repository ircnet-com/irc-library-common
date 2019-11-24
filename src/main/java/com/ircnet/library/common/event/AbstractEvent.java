package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;

import java.util.EventObject;

public abstract class AbstractEvent<T extends IRCConnection> extends EventObject {
    protected T ircConnection;

    public AbstractEvent() {
        this(new Object());
    }

    public AbstractEvent(Object source) {
        super(source);
    }

    public T getIRCConnection() {
        return ircConnection;
    }

    public void setIRCConnection(T ircConnection) {
        this.ircConnection = ircConnection;
    }
}
