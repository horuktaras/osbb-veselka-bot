package ua.horuktaras.osbb.bot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.horuktaras.osbb.bot.moderation.FloodControl;

@Component
public class RateLimitCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RateLimitCleanupScheduler.class);

    private final FloodControl floodControl;

    public RateLimitCleanupScheduler(FloodControl floodControl) {
        this.floodControl = floodControl;
    }

    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void cleanup() {
        log.debug("Running FloodControl cleanup");
        floodControl.cleanupExpired();
    }
}
