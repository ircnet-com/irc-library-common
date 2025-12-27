package com.ircnet.library.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class User implements Comparable<User> {
    private String nick;
    private String user;
    private String host;

    public User(String from) {
        String hostmask = Util.removeLeadingColon(from);

        if (hostmask.contains("!")) {
            final int index1 = hostmask.indexOf("!");
            final int index2 = hostmask.indexOf("@");

            this.nick = hostmask.substring(hostmask.charAt(0) == ':' ? 1 : 0, index1);
            this.user = hostmask.substring(index1 + 1, index2);
            this.host = hostmask.substring(index2 + 1);
        }
        else if(hostmask.contains("@") && hostmask.contains("[") && hostmask.endsWith("]")) {
            int openingSquareBracketIndex = from.indexOf("[");
            int atSignIndex = from.indexOf("@");
            this.nick = Util.removeLeadingColon(from.substring(0, openingSquareBracketIndex));
            this.user = from.substring(openingSquareBracketIndex + 1, atSignIndex);
            this.host = from.substring(atSignIndex+1, from.indexOf("]"));
        }
        else {
            this.nick = hostmask;
        }
    }

    @Override
    public int compareTo(User o) {
        if (this.nick == null || o.getNick() == null) {
            return 0;
        }
        return this.nick.compareToIgnoreCase(o.getNick());
    }

    @Override
    public String toString() {
        return (nick != null ? nick : "") + "!" + (user != null ? user : "") + "@" + (host != null ? host : "");
    }
}