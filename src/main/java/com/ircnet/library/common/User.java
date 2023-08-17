package com.ircnet.library.common;

public class User implements Comparable<User> {
    private String nick;
    private String user;
    private String host;

    public User() {
    }

    public User(String from) {
        String hostmask = Util.removeLeadingColon(from);

        if (hostmask.contains("!")) {
            final int index1 = hostmask.indexOf("!");
            final int index2 = hostmask.indexOf("@");

            this.nick = hostmask.substring(hostmask.charAt(0) == ':' ? 1 : 0, index1);
            this.user = hostmask.substring(index1 + 1, index2);
            this.host = hostmask.substring(index2 + 1);
        }

        else if(hostmask.contains("@") && hostmask.contains("[") && hostmask.endsWith("]")) { // TODO: regex :)
            int openingSquareBracketIndex = from.indexOf("[");
            int atSignIndex = from.indexOf("@");
            this.nick = Util.removeLeadingColon(from.substring(0, openingSquareBracketIndex));
            this.user = from.substring(openingSquareBracketIndex + 1, atSignIndex);
            this.host = from.substring(atSignIndex+1, from.indexOf("]"));
        }

        else
            this.nick = hostmask;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int compareTo(User o) {
        return this.getNick().compareToIgnoreCase(o.getNick());
    }

    @Override
    public String toString() {
        return getNick() + "!" + getUser() + "@" + getHost();
    }
}
