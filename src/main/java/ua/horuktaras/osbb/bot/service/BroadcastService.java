package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    private static final long DELAY_BETWEEN_SENDS_MS = 50;

    private final TelegramApiService telegramApiService;
    private final ExecutorService virtualThreadExecutor;

    public record BroadcastResult(int successCount, int failureCount, List<Long> failedChatIds) {}

    public BroadcastService(
            TelegramApiService telegramApiService,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor
    ) {
        this.telegramApiService = telegramApiService;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    public BroadcastResult broadcast(String message, List<Long> chatIds) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> failedChatIds = new java.util.concurrent.CopyOnWriteArrayList<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < chatIds.size(); i++) {
            Long chatId = chatIds.get(i);
            long delayMs = (long) i * DELAY_BETWEEN_SENDS_MS;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text(message)
                        .parseMode("HTML")
                        .build();
                var result = telegramApiService.sendMessage(sendMessage);
                if (result.isPresent()) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    failedChatIds.add(chatId);
                    log.warn("Broadcast failed to chatId={}", chatId);
                }
            }, virtualThreadExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int success = successCount.get();
        int failure = failureCount.get();
        log.info("Broadcast complete: success={} failure={}", success, failure);
        return new BroadcastResult(success, failure, failedChatIds);
    }
}
