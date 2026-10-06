package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ua.horuktaras.osbb.bot.model.entity.TelegramUser;
import ua.horuktaras.osbb.bot.repository.TelegramUserRepository;

@Service
public class TelegramUserService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUserService.class);

    private final TelegramUserRepository telegramUserRepository;

    public TelegramUserService(TelegramUserRepository telegramUserRepository) {
        this.telegramUserRepository = telegramUserRepository;
    }

    @Transactional
    public TelegramUser getOrCreate(User user) {
        return telegramUserRepository.findByTelegramUserId(user.getId())
                .map(existing -> {
                    boolean changed = false;
                    if (!equalOrBothNull(existing.getUsername(), user.getUserName())) {
                        existing.setUsername(user.getUserName());
                        changed = true;
                    }
                    if (!equalOrBothNull(existing.getFirstName(), user.getFirstName())) {
                        existing.setFirstName(user.getFirstName());
                        changed = true;
                    }
                    if (!equalOrBothNull(existing.getLastName(), user.getLastName())) {
                        existing.setLastName(user.getLastName());
                        changed = true;
                    }
                    if (changed) {
                        return telegramUserRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    log.debug("Creating new TelegramUser for userId={}", user.getId());
                    TelegramUser newUser = new TelegramUser();
                    newUser.setTelegramUserId(user.getId());
                    newUser.setUsername(user.getUserName());
                    newUser.setFirstName(user.getFirstName());
                    newUser.setLastName(user.getLastName());
                    return telegramUserRepository.save(newUser);
                });
    }

    @Transactional
    public TelegramUser update(User user) {
        TelegramUser entity = telegramUserRepository.findByTelegramUserId(user.getId())
                .orElseGet(() -> {
                    TelegramUser u = new TelegramUser();
                    u.setTelegramUserId(user.getId());
                    return u;
                });
        entity.setUsername(user.getUserName());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        return telegramUserRepository.save(entity);
    }

    private boolean equalOrBothNull(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
