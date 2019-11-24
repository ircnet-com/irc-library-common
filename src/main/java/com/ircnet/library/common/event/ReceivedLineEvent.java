package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;

public class ReceivedLineEvent extends AbstractEvent {
    private String line;

    public ReceivedLineEvent(IRCConnection ircConnection, String line) {
        this.ircConnection = ircConnection;
        this.line = line;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }
}
