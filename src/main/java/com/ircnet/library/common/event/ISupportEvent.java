package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class ISupportEvent<T extends IRCConnection> extends AbstractEvent<T> {
    private final Map<String, String> keyValuePair;
}
