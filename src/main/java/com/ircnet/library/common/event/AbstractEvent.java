package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractEvent<T extends IRCConnection> {
    @NonNull
    protected final EventContext<T> context;

    protected String raw;

    public T getIRCConnection() {
        return context.getIrcConnection();
    }

    public long getLineSeq() {
        return context.getLineSeq();
    }
}
