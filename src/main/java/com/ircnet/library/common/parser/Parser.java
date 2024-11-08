package com.ircnet.library.common.parser;

import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;

public interface Parser<T extends IRCConnection> {
    boolean parse(T ircConnection, String line);
    void setIRCConnectionService(IRCConnectionService ircConnectionService);
}
