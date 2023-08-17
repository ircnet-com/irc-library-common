package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Data;
import lombok.Getter;

@Getter
public class ReceivedLineEvent extends AbstractEvent {
    private String line;

    public ReceivedLineEvent(IRCConnection ircConnection, String line) {
        this.ircConnection = ircConnection;
        this.line = line;
    }
}
