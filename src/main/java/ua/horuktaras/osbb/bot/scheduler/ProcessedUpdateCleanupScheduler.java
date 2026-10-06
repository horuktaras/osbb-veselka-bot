package ua.horuktaras.osbb.bot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.horuktaras.osbb.bot.service.UpdateIdempotencyService;

@Component
public class ProcessedUpdateCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProcessedUpdateCleanupScheduler.class);

    private final UpdateIdempotencyService updateIdempotencyService;

    public ProcessedUpdateCleanupScheduler(UpdateIdempotencyService updateIdempotencyService) {
        this.updateIdempotencyService = updateIdempotencyService;
    }

    @Scheduled(fixedDelay = 3600000) // every hour
    public void cleanup() {
        log.debug("Running ProcessedUpdate cleanup");
        updateIdempotencyService.cleanupOldEntries();
    }
}
