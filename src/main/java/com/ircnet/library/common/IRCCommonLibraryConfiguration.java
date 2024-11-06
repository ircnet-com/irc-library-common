package com.ircnet.library.common;

import com.ircnet.library.common.connection.ResolveService;
import com.ircnet.library.common.event.EventBus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IRCCommonLibraryConfiguration {
    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    @Bean
    public SettingService settingService(@Value("${lagcheck.interval:#{null}}") Integer defaultLagcheckInterval,
                                         @Value("${lagcheck.max-lag-before-disconnect:#{null}}") Integer defaultMaxLagBeforeDisconnect) {
        return new SettingServiceImpl(
            defaultLagcheckInterval != null ? defaultLagcheckInterval : SettingConstants.LAGCHECK_INTERVAL_DEFAULT,
            defaultMaxLagBeforeDisconnect != null ? defaultMaxLagBeforeDisconnect : SettingConstants.MAX_LAG_BEFORE_DISCONNECT_DEFAULT);
    }

    @Bean
    public ResolveService resolveService() {
        return new ResolveService();
    }
}
