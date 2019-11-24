package com.ircnet.library.common.configuration;

import java.util.List;

public interface ConfigurationModel {
    String getUserId();

    List<IRCServerModel> getIrcServers();

    String getLocalAddress();

    boolean isAutoConnectEnabled();
}
