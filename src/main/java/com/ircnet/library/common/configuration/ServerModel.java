package com.ircnet.library.common.configuration;

public abstract class ServerModel {
    public enum Protocol {
        IPV4, IPV6,
        ipv4,ipv6 // FIXME legacy
    }

    protected String hostname;
    protected int port;
    protected Protocol protocol;
    protected boolean ssl;

    public ServerModel() {
    }

    public ServerModel(String hostname, int port, Protocol protocol) {
        this.hostname = hostname;
        this.port = port;
        this.protocol = protocol;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
}
