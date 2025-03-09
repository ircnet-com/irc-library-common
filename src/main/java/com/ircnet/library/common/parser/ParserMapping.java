package com.ircnet.library.common.parser;

import com.ircnet.library.common.connection.IRCConnection;
import lombok.Getter;

@Getter
public class ParserMapping<T extends IRCConnection> {
    private String key;
    private int index;
    private int argumentCount;
    private ParserMethod<T> parserMethod;

    public ParserMapping(String key, int index, int argumentCount, ParserMethod<T> parserMethod) {
        this.key = key;
        this.index = index;
        this.argumentCount = argumentCount;
        this.parserMethod = parserMethod;
    }
}
