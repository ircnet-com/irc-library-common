package com.ircnet.library.common.parser;

import com.ircnet.library.common.Util;
import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParserImpl<T extends IRCConnection> implements Parser<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserImpl.class);

    protected IRCConnectionService ircConnectionService;
    protected List<ParserMapping<T>> parserMappingList;

    public ParserImpl() {
        parserMappingList = new ArrayList<>();
        parserMappingList.add(new ParserMapping<>("PING", 0, 2, (arg1, arg2, arg3) -> parsePing(arg1, arg2)));
        parserMappingList.add(new ParserMapping<>("PONG", 1, 4, (arg1, arg2, arg3) -> parsePong(arg1, arg2)));
        parserMappingList.add(new ParserMapping<>("ERROR", 0, 2, (arg1, arg2, arg3) -> parseError(arg1, arg2)));
    }

    @Override
    public void setIRCConnectionService(IRCConnectionService ircConnectionService) {
        this.ircConnectionService = ircConnectionService;
    }

    @Override
    public boolean parse(T ircConnection, String line) {
        String[] parts = line.split(" ");

        for(ParserMapping parserMapping : parserMappingList) {
            if(parts.length > parserMapping.getIndex() && parserMapping.getKey().equals(parts[parserMapping.getIndex()])) {
                parts = line.split(" ", parserMapping.getArgumentCount());
                parserMapping.getParserMethod().parse(ircConnection, parts, new HashMap<>());
                return true;
            }
        }

        return false;
    }

    protected void parsePing(T ircConnection, String[] parts) {
        ircConnectionService.send(ircConnection, "PONG %s", parts[1]);
    }

    protected void parsePong(T ircConnection, String[] parts) {
        ircConnectionService.handleLagCheckResponse(ircConnection, parts[3].substring(1));
    }

    private void parseError(T ircConnection, String[] parts) {
        LOGGER.info("Received error: {}", Util.removeLeadingColon(parts[1]));
    }
}
