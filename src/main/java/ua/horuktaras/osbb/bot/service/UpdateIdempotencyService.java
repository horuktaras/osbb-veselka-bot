package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.ProcessedUpdate;
import ua.horuktaras.osbb.bot.repository.ProcessedUpdateRepository;

import java.time.Instant;

@Service
public class UpdateIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(UpdateIdempotencyService.class);

    private final ProcessedUpdateRepository processedUpdateRepository;

    public UpdateIdempotencyService(ProcessedUpdateRepository processedUpdateRepository) {
        this.processedUpdateRepository = processedUpdateRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(Integer updateId) {
        return processedUpdateRepository.existsByUpdateId(updateId);
    }

    @Transactional
    public void markProcessed(Integer updateId) {
        if (!processedUpdateRepository.existsByUpdateId(updateId)) {
            processedUpdateRepository.save(new ProcessedUpdate(updateId));
        }
    }

    @Scheduled(fixedDelay = 3600000) // every hour
    @Transactional
    public void cleanupOldEntries() {
        Instant cutoff = Instant.now().minusSeconds(86400); // 24 hours
        int deleted = processedUpdateRepository.deleteByProcessedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old processed update records", deleted);
        }
    }
}
