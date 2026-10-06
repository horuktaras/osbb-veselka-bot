package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;

import java.util.Optional;

@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {
    Optional<ChatConfig> findByChatId(Long chatId);
}
