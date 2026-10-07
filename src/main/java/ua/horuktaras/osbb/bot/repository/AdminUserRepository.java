package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.horuktaras.osbb.bot.model.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    boolean existsByTelegramId(Long telegramId);
}
