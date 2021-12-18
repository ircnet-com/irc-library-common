package com.ircnet.library.common;

public class Util {
    private static final String REGEX_SPECIAL_CHARACTERS = "<([{\\^-=$!|]})+.>";

    public static String removeLeadingColon(String text) {
        if(text == null || text.isEmpty())
            return "";

        if(text.charAt(0) == ':')
            return text.substring(1);

        return text;
    }

    public static boolean hostmaskMatches(String pattern, String hostmask) {
        StringBuilder stringBuilder = new StringBuilder();

        for (char c : pattern.toCharArray()) {
            if (REGEX_SPECIAL_CHARACTERS.indexOf(c) != -1) {
                stringBuilder.append("\\");
            }

            if (c == '?')
                stringBuilder.append(".");

            else if (c == '*')
                stringBuilder.append(".*");
            else
                stringBuilder.append(c);
        }

        return hostmask.matches(stringBuilder.toString());
    }

    public static boolean hostmaskMatches(String pattern, User user) {
        return hostmaskMatches(pattern, user.getNick() + "!" + user.getUser() + "@" + user.getHost());
    }
}
