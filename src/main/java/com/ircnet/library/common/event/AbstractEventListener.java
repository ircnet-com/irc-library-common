package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnectionService;

import java.lang.reflect.ParameterizedType;
import java.util.EventListener;

public abstract class AbstractEventListener<T extends AbstractEvent, T2 extends IRCConnectionService> implements EventListener {
    protected T2 ircConnectionService;

    private String owner;
    private Class<T> eventClass;

    public AbstractEventListener(T2 ircConnectionService) {
        this.ircConnectionService = ircConnectionService;
        this.eventClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected abstract void onEvent(T event);

    public Class<T> getEventClass() {
        return eventClass;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
