package ua.horuktaras.osbb.bot.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.horuktaras.osbb.bot.model.entity.ChatMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    Optional<ChatMember> findByChatIdAndTelegramUserId(Long chatId, Long telegramUserId);

    List<ChatMember> findByChatId(Long chatId);

    Long countByChatIdAndWarningCountGreaterThan(Long chatId, int warningCount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cm FROM ChatMember cm WHERE cm.chatId = :chatId AND cm.telegramUserId = :telegramUserId")
    Optional<ChatMember> findByChatIdAndTelegramUserIdForUpdate(
            @Param("chatId") Long chatId,
            @Param("telegramUserId") Long telegramUserId
    );
}
