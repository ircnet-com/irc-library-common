package com.ircnet.library.common;

import java.util.List;

public interface IRCTaskService {
    void run(IRCTask ircTask);
    void run(List<? extends IRCTask> ircTasks);
}
