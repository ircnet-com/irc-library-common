package com.ircnet.library.common.connection;

import com.ircnet.library.common.IRCTask;
import com.ircnet.library.common.SettingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class LagCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger(LagCheck.class);

    private boolean inProgress;
    private int lag;
    private Date sent;
    private Date next;

    public LagCheck() {
    }

    public boolean shouldPerformLagCheck(IRCConnection ircConnection) {
        return !inProgress && ircConnection.getPenalty() == 0 && this.getNext() != null && System.currentTimeMillis() >= this.getNext().getTime();
    }

    public void checkLag(IRCConnection ircConnection) {
        Date now = new Date();
        ircConnection.send("PING :%s", now.getTime());
        this.inProgress = true;
        this.sent = now;
        this.next = null;
    }

    public void handleLagCheckResponse(IRCConnection ircConnection, String data) {
        if (inProgress && sent.getTime() == Long.valueOf(data)) {
            this.lag = (int) ((System.currentTimeMillis() - this.sent.getTime()) / 1000);
            LOGGER.debug("Current lag is {} seconds", this.lag);
            this.inProgress = false;
        }

        this.next = new Date(System.currentTimeMillis() + IRCTask.getSettingService().findInteger(SettingConstants.LAGCHECK_INTERVAL, SettingConstants.LAGCHECK_INTERVAL_DEFAULT) * 1000);
    }

    public int getLag() {
        if (inProgress) {
            lag = (int) ((System.currentTimeMillis() - this.sent.getTime()) / 1000);
        }

        return lag;
    }

    public void reset() {
        this.inProgress = false;
        this.lag = 0;
        this.sent = null;
        this.next = null;
    }

    public Date getNext() {
        return next;
    }

    public void setNext(Date next) {
        this.next = next;
    }
}
