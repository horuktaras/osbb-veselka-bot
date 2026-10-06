package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.horuktaras.osbb.bot.model.entity.ModerationLog;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    Page<ModerationLog> findByChatIdOrderByCreatedAtDesc(Long chatId, Pageable pageable);

    Page<ModerationLog> findByChatIdAndTargetTelegramUserIdOrderByCreatedAtDesc(
            Long chatId, Long targetTelegramUserId, Pageable pageable);
}
