package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class MyInfoEvent<T extends IRCConnection> extends AbstractEvent<T> {
    private final String serverName;
    private final String version;
    private final String availableUserModes;
    private final String availableChannelModes;
}
