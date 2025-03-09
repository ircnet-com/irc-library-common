package com.ircnet.library.common.connection;

import com.ircnet.library.common.SettingService;
import com.ircnet.library.common.event.EventBus;
import com.ircnet.library.common.parser.Parser;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Formatter;

public class SingletonIRCConnectionServiceImpl extends IRCConnectionServiceImpl implements SingletonIRCConnectionService {
    private IRCConnection ircConnection;

    public SingletonIRCConnectionServiceImpl(EventBus eventBus,
                                             Parser parser,
                                             SettingService settingService,
                                             ResolveService resolveService,
                                             IRCConnection ircConnection) {
        super(eventBus, parser, settingService, resolveService);
        this.ircConnection = ircConnection;
    }

    public IRCConnection getIrcConnection() {
        return ircConnection;
    }

    @Override
    public void send(String format, Object... args) {
        String text;

        if(ArrayUtils.isNotEmpty(args)) {
            text = new Formatter().format(format, args).toString();
        }
        else {
            text = format;
        }

        super.send(ircConnection, text);
    }

    @Override
    public void send(String text) {
        super.send(ircConnection, text);
    }

    @Override
    public void notice(String target, String format, Object... args) {
        String text;

        if(ArrayUtils.isNotEmpty(args)) {
            text = new Formatter().format(format, args).toString();
        }
        else {
            text = format;
        }

        super.notice(ircConnection, target, text);
    }
}
