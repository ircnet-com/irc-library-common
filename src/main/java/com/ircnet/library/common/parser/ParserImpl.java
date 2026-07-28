 package com.ircnet.library.common.parser;

import com.ircnet.library.common.Util;
import com.ircnet.library.common.connection.IRCConnection;
import com.ircnet.library.common.connection.IRCConnectionService;
import com.ircnet.library.common.connection.ISupport;
import com.ircnet.library.common.event.EventBus;
import com.ircnet.library.common.event.EventContext;
import com.ircnet.library.common.event.ISupportEvent;
import com.ircnet.library.common.event.MyInfoEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 public class ParserImpl<T extends IRCConnection> implements Parser<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParserImpl.class);

     protected final EventBus eventBus;

     protected final List<ParserMapping<T>> parserMappingList;
     protected IRCConnectionService ircConnectionService;

     public ParserImpl(EventBus eventBus,
                       @Lazy IRCConnectionService ircConnectionService) {
         this.eventBus = eventBus;
         this.ircConnectionService = ircConnectionService;
         parserMappingList = new ArrayList<>();
         parserMappingList.add(new ParserMapping<>("PING", 0, (arg1, arg2, arg3, arg4, arg5) -> parsePing(arg1, arg2)));
         parserMappingList.add(new ParserMapping<>("PONG", 1, (arg1, arg2, arg3, arg4, arg5) -> parsePong(arg1, arg2)));
         parserMappingList.add(new ParserMapping<>("ERROR", 0, (arg1, arg2, arg3, arg4, arg5) -> parseError(arg1, arg2)));
         parserMappingList.add(new ParserMapping<>("004", 1, (arg1, arg2, arg3, arg4, arg5) -> parseMyInfo(arg1, arg2, arg4, arg5)));
         parserMappingList.add(new ParserMapping<>("005", 1, (arg1, arg2, arg3, arg4, arg5) -> parseISupport(arg1, arg2, arg4, arg5)));
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

     private void parseMyInfo(T ircConnection,
                                String[] parts,
                                EventContext<T> eventContext,
                                String line) {
         ircConnection.setVersion(parts[4]);

         if(ircConnection.getVersion().startsWith("2.12.")) {
             ircConnection.setVersion212(true);
         }

         eventBus.publishEvent(MyInfoEvent.<T>builder()
             .context(eventContext)
             .serverName(parts[3])
             .version(parts[4])
             .availableUserModes(parts[5])
             .availableChannelModes(parts[6])
             .raw(line)
             .build());
     }

     private void parseISupport(T ircConnection,
                                String[] parts,
                                EventContext<T> eventContext,
                                String line) {
         Map<String, String> keyValuePair = parseISupportParameters(parts);

         ISupport iSupport = ircConnection.getISupport();
         applyISupportParameters(iSupport, keyValuePair);

         eventBus.publishEvent(ISupportEvent.<T>builder()
             .context(eventContext)
             .keyValuePair(keyValuePair)
             .raw(line)
             .build());
     }

     private Map<String, String> parseISupportParameters(String[] parts) {
         Map<String, String> keyValuePair = new HashMap<>();

         for (int i = 3; i < parts.length; i++) {
             String part = parts[i];
             int index = part.indexOf('=');

             if (index != -1) {
                 String key = part.substring(0, index);
                 String value = part.substring(index + 1);

                 keyValuePair.put(key, value);
             } else {
                 keyValuePair.put(part, null);
             }
         }

         return keyValuePair;
     }

     private void applyISupportParameters(ISupport iSupport, Map<String, String> keyValuePair) {
         String value;

         if ((value = keyValuePair.get("CHANMODES")) != null) {
             iSupport.parseSupportedChanModes(value);
         }

         if ((value = keyValuePair.get("CHANLIMIT")) != null) {
             iSupport.parseChanLimit(value);
         }

         if ((value = keyValuePair.get("MAXCHANNELS")) != null) {
             iSupport.parseMaxChannels(value);
         }

         if ((value = keyValuePair.get("PREFIX")) != null) {
             iSupport.parsePrefix(value);
         }

         if ((value = keyValuePair.get("CHANTYPES")) != null) {
             iSupport.setChannelTypes(value);
         }

         if ((value = keyValuePair.get("CHANNELLEN")) != null) {
             iSupport.setChannelLength(Integer.parseInt(value));
         }
     }
}
