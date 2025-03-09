package com.ircnet.library.common.configuration;

import lombok.Data;

@Data
public abstract class ServerModel {
    public enum Protocol {
        IPV4,
        IPV6
    }

    protected String address;
    protected int port;
    protected Protocol protocol;
    protected String password;
    protected boolean ssl;

    public ServerModel() {
    }
}
