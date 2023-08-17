package com.ircnet.library.common.connection;

public interface SingletonIRCConnectionService extends IRCConnectionService {
    /**
     * @see IRCConnectionService#send(IRCConnection, String, Object...)}
     */
    void send(String format, Object... args);

    /**
     * @see IRCConnectionService#send(IRCConnection, String)
     */
    void send(String text);

    /**
     * @see IRCConnectionService#notice(IRCConnection, String, String, Object...)
     */
    void notice(String target, String format, Object... args);
}
