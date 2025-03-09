package com.ircnet.library.common.connection;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 005
 */
@Getter
@Setter
public class ISupport {
    private Map<Character, Character> channelModes;
    private int maxChannels;
    private String channelStatusFlags;
    private String channelTypes;

    public ISupport() {
        this.channelModes = new HashMap<>();
    }

    /**
     * Determines the type of a channel mode.
     * Available types are: A, B, C, D which are described in {@link ISupport#checkChannelModeRequiresArgument(char, char)}.
     *
     * @param mode A channel mode
     * @return Type of the channel mode as char or '-' if an error occurred
     */
    public char getTypeOfChannelMode(char mode) {
        Character result = channelModes.get(mode);

        if(result != null) {
            return result;
        }
        else {
            return '-';
        }
    }

    /**
     * Checks if a channel mode requires an argument.
     *
     * @param sign '+' or '-'
     * @param mode A channel mode
     * @return true if the channel mode requires an argument, otherwise false
     */
    public boolean checkChannelModeRequiresArgument(char sign, char mode) {
        // 'o', 'v'
        if(this.channelStatusFlags != null && this.channelStatusFlags.indexOf(mode) != -1) {
            return true;
        }

        char type = getTypeOfChannelMode(mode);

        switch(type) {
            case '-': // type not found
                return false;
            case 'A':
                   /* "Type A: Modes that add or remove an address to or from a list.
                               These modes MUST always have a parameter when sent from the server
                               to a client."
                   */
                return true;

            case 'B':
                   /* "Type B: Modes that change a setting on a channel.  These modes
                               MUST always have a parameter."
                   */
                return true;

            case 'C':
                   /* "Type C: Modes that change a setting on a channel.  These modes
                               MUST have a parameter when being set, and MUST NOT have a
                               parameter when being unset."
                   */

                if(sign == '+') {
                    return true;
                }
                else {
                    return false;
                }

            case 'D':
                   /* "Type D: Modes that change a setting on a channel.  These modes
                               MUST NOT have a parameter."
                   */
                return false;
        }

        return false;
    }

    /**
     * Determines if a string is a channel according to CHANTYPES.
     *
     * @param name A string
     * @return true if the string is a channel according to CHANTYPES
     */
    public boolean isChannel(String name) {
        if(StringUtils.isEmpty(this.channelTypes) || StringUtils.isEmpty(name)) {
            return false;
        }

        return channelTypes.indexOf(name.charAt(0)) != -1;
    }

    /**
     * Parses CHANMODES, e.g. "CHANMODES=beIR,k,l,imnpstaqr".
     *
     * @param value Value of CHANMODES, e.g. "beIR,k,l,imnpstaqr"
     */
    public void parseSupportedChanModes(String value) {
        char[] types = {'A', 'B', 'C', 'D'};

        String[] modes = value.split(",");

        for(int i = 0; i < modes.length; i++) {
            if(i >= types.length) {
                return;
            }

            for(int j = 0; j < modes[i].length(); j++)
                getChannelModes().put(modes[i].charAt(j), types[i]);
        }
    }

    /**
     * Parses CHANLIMIT, e.g. "CHANLIMIT=#&!+:42".
     *
     * @param value Value of CHANLIMIT, e.g. "#&!+:42"
     */
    public void parseChanLimit(String value) {
        String maxValue = value.substring(value.indexOf(':') + 1);

        if(StringUtils.isBlank(maxValue)) {
            setMaxChannels(40);
        }
        else {
            setMaxChannels(Integer.parseInt(maxValue));
        }
    }

    /**
     * Parses MAXCHANNELS, e.g. "MAXCHANNELS=20"
     *
     * @param value Value of MAXCHANNELS, e.g. "20"
     */
    public void parseMaxChannels(String value) {
        setMaxChannels(Integer.parseInt(value));
    }

    /**
     * Parses PREFIX, e.g. "PREFIX=(ov)@+".
     *
     * @param value Value of PREFIX, e.g. "(ov)@+"
     */
    public void parsePrefix(String value) {
        int modesStart = value.indexOf('(') + 1;
        int modesEnd = value.indexOf(')');

        if(modesEnd > modesStart) {
            this.channelStatusFlags = value.substring(modesStart, modesEnd);
        }
    }
}
