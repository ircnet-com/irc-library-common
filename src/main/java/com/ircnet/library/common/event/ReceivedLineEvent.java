package com.ircnet.library.common.event;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;

@Getter
public class ReceivedLineEvent extends AbstractEvent {
    private String line;

    public ReceivedLineEvent(EventContext context, String line) {
        super(context);
        this.line = line;
    }
}
