package com.ircnet.library.common;

import com.ircnet.library.common.configuration.ConfigurationModel;
import com.ircnet.library.common.connection.IRCConnection;

public interface Client<S extends IRCConnection, T extends ConfigurationModel> {
    S getIRCConnection();
    T getConfiguration();
}
