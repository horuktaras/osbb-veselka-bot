package ua.horuktaras.osbb.bot.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;
import ua.horuktaras.osbb.bot.repository.VerificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class VerificationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VerificationRepository verificationRepository;

    @Test
    void saveAndFindPendingVerification() {
        Verification v = new Verification();
        v.setChatId(100L);
        v.setTelegramUserId(200L);
        v.setStatus(VerificationStatus.PENDING);
        v.setCreatedAt(Instant.now());
        v.setExpiresAt(Instant.now().plusSeconds(600));
        verificationRepository.save(v);

        Optional<Verification> found = verificationRepository.findByChatIdAndTelegramUserIdAndStatus(
                100L, 200L, VerificationStatus.PENDING);
        assertTrue(found.isPresent());
        assertEquals(VerificationStatus.PENDING, found.get().getStatus());
    }

    @Test
    void findExpiredVerifications() {
        Verification v = new Verification();
        v.setChatId(300L);
        v.setTelegramUserId(400L);
        v.setStatus(VerificationStatus.PENDING);
        v.setCreatedAt(Instant.now().minusSeconds(1200));
        v.setExpiresAt(Instant.now().minusSeconds(600)); // expired
        verificationRepository.save(v);

        List<Verification> expired = verificationRepository.findAllByStatusAndExpiresAtBefore(
                VerificationStatus.PENDING, Instant.now());
        assertFalse(expired.isEmpty());
        assertTrue(expired.stream().anyMatch(ev -> ev.getChatId().equals(300L)));
    }

    @Test
    void statusUpdate() {
        Verification v = new Verification();
        v.setChatId(500L);
        v.setTelegramUserId(600L);
        v.setStatus(VerificationStatus.PENDING);
        v.setCreatedAt(Instant.now());
        v.setExpiresAt(Instant.now().plusSeconds(600));
        Verification saved = verificationRepository.save(v);

        saved.setStatus(VerificationStatus.VERIFIED);
        saved.setCompletedAt(Instant.now());
        verificationRepository.save(saved);

        Optional<Verification> found = verificationRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(VerificationStatus.VERIFIED, found.get().getStatus());
        assertNotNull(found.get().getCompletedAt());
    }
}
