package com.ircnet.library.common.connection;

import java.util.Formatter;

public class SingletonIRCConnectionServiceImpl extends IRCConnectionServiceImpl implements SingletonIRCConnectionService {
    private IRCConnection ircConnection;

    public SingletonIRCConnectionServiceImpl(IRCConnection ircConnection) {
        this.ircConnection = ircConnection;
    }

    public IRCConnection getIrcConnection() {
        return ircConnection;
    }

    @Override
    public void send(String format, Object... args) {
        super.send(ircConnection, new Formatter().format(format, args).toString());
    }

    @Override
    public void send(String text) {
        super.send(ircConnection, text);
    }

    @Override
    public void notice(String target, String format, Object... args) {
        super.notice(ircConnection, target, new Formatter().format(format, args).toString());
    }
}
