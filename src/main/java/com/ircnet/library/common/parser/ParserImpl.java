 package com.ircnet.library.common.parser;

import com.ircnet.library.common.Util;
import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import com.ircnet.library.common.event.EventContext;
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
        parserMappingList.add(new ParserMapping<>("PING", 0, (arg1, arg2, arg3, arg4, arg5) -> parsePing(arg1, arg2)));
        parserMappingList.add(new ParserMapping<>("PONG", 1, (arg1, arg2, arg3, arg4, arg5) -> parsePong(arg1, arg2)));
        parserMappingList.add(new ParserMapping<>("ERROR", 0, (arg1, arg2, arg3, arg4, arg5) -> parseError(arg1, arg2)));
    }

    @Override
    public boolean parse(T ircConnection, String input, EventContext<T> eventContext) {
        String line;
        Map<String, String> tagMap = new HashMap<>();

        if(input.charAt(0) == '@') {
            tagMap.putAll(parseMessageTags(input));
            line = StringUtils.substringAfter(input, " ");
        }
        else {
            line = input;
        }

        String[] parts = line.split(" ", countParams(input));

        for(ParserMapping<T> parserMapping : parserMappingList) {
            if(parts.length > parserMapping.getIndex() && parserMapping.getKey().equals(parts[parserMapping.getIndex()])) {
                parserMapping.getParserMethod().parse(ircConnection, parts, tagMap, eventContext, line);
                return true;
            }
        }

        return false;
    }

    private int countParams(String input) {
        int count = 1;

        if(input.length() < 2) {
            return count;
        }

        char[] inputArray = input.toCharArray();

        for(int i = 1; i < inputArray.length; i++) {
            if(inputArray[i] == ' ') {
                count++;

                if(i + 1 < inputArray.length && inputArray[i + 1] == ':') {
                    break;
                }
            }
        }

        return count;
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
