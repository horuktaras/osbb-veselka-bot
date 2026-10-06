package ua.horuktaras.osbb.bot.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ua.horuktaras.osbb.bot.model.entity.ChatMember;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;
import ua.horuktaras.osbb.bot.repository.ChatMemberRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ChatMemberRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ChatMemberRepository chatMemberRepository;

    @Test
    void saveAndFindChatMember() {
        ChatMember member = new ChatMember();
        member.setChatId(11111L);
        member.setTelegramUserId(22222L);
        member.setVerificationStatus(VerificationStatus.PENDING);

        ChatMember saved = chatMemberRepository.save(member);
        assertNotNull(saved.getId());

        Optional<ChatMember> found = chatMemberRepository.findByChatIdAndTelegramUserId(11111L, 22222L);
        assertTrue(found.isPresent());
        assertEquals(VerificationStatus.PENDING, found.get().getVerificationStatus());
    }

    @Test
    void uniqueConstraintEnforced() {
        Long chatId = 33333L;
        Long userId = 44444L;

        ChatMember member1 = new ChatMember();
        member1.setChatId(chatId);
        member1.setTelegramUserId(userId);
        member1.setVerificationStatus(VerificationStatus.VERIFIED);
        chatMemberRepository.save(member1);

        ChatMember member2 = new ChatMember();
        member2.setChatId(chatId);
        member2.setTelegramUserId(userId);
        member2.setVerificationStatus(VerificationStatus.PENDING);

        assertThrows(DataIntegrityViolationException.class, () -> chatMemberRepository.saveAndFlush(member2));
    }

    @Test
    void warningCountUpdateWithVersion() {
        ChatMember member = new ChatMember();
        member.setChatId(55555L);
        member.setTelegramUserId(66666L);
        member.setVerificationStatus(VerificationStatus.VERIFIED);
        ChatMember saved = chatMemberRepository.save(member);

        assertEquals(0L, saved.getVersion());

        saved.setWarningCount(1);
        ChatMember updated = chatMemberRepository.save(saved);

        assertEquals(1, updated.getWarningCount());
        assertEquals(1L, updated.getVersion());
    }
}
