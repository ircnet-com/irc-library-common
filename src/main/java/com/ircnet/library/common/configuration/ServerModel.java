package com.ircnet.library.common.configuration;

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

    public ServerModel(String address, int port, Protocol protocol) {
        this.address = address;
        this.port = port;
        this.protocol = protocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
