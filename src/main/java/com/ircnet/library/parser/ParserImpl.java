package com.ircnet.library.parser;

import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ParserImpl<T extends IRCConnection> implements Parser<T> {
    @Autowired
    private IRCConnectionService ircConnectionService;

    private List<ParserMapping<T>> parserMappingList;

    public ParserImpl() {
        parserMappingList = new ArrayList<>();
        parserMappingList.add(new ParserMapping<>("PING", 0, 2, (arg1, arg2) -> parsePing(arg1, arg2)));
        parserMappingList.add(new ParserMapping<>("PONG", 1, 4, (arg1, arg2) -> parsePong(arg1, arg2)));
    }

    @Override
    public void parse(T ircConnection, String line) {
        String[] parts = line.split(" ");

        for(ParserMapping parserMapping : parserMappingList) {
            if(parts.length > parserMapping.getIndex() && parserMapping.getKey().equals(parts[parserMapping.getIndex()])) {
                parts = line.split(" ", parserMapping.getArgumentCount());
                parserMapping.getParserMethod().parse(ircConnection, parts);
                return;
            }
        }
    }

    protected void parsePing(T ircConnection, String[] parts) {
        ircConnectionService.send(ircConnection, "PONG %s", parts[1]);
    }

    protected void parsePong(T ircConnection, String[] parts) {
        // :fu-berlin.de PONG fu-berlin.de :foobar
        ircConnectionService.handleLagCheckResponse(ircConnection, parts[3].substring(1));
    }
}
