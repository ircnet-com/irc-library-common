 package com.ircnet.library.common.parser;

import com.ircnet.library.common.Util;
import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public boolean parse(T ircConnection, String input) {
        String line;
        Map<String, String> tagMap = new HashMap<>();

        if(input.charAt(0) == '@') {
            tagMap.putAll(parseMessageTags(input));
            line = StringUtils.substringAfter(input, " ");
        }
        else {
            line = input;
        }

        String[] parts = line.split(" ");

        for(ParserMapping parserMapping : parserMappingList) {
            if(parts.length > parserMapping.getIndex() && parserMapping.getKey().equals(parts[parserMapping.getIndex()])) {
                parts = line.split(" ", parserMapping.getArgumentCount());
                parserMapping.getParserMethod().parse(ircConnection, parts, tagMap);
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

     /**
      * Creates a map of message tags from messages like:
      *   "@aaa=bbb;ccc;example.com/ddd=eee :nick!ident@host PRIVMSG me :Hello"
      *
      * @param line
      * @return a message of tags
      */
     private Map<String, String> parseMessageTags(String line) {
         Map<String, String> tagMap = new HashMap<>();
         String[] parts = line.split(" ", 2);
         String[] tags = parts[0].substring(1).split(";");

         for (String tag : tags) {
             String[] keyAndValue = tag.split("=");
             tagMap.put(keyAndValue[0], keyAndValue.length > 1 ? keyAndValue[1] : null);
         }

         return tagMap;
     }
}
