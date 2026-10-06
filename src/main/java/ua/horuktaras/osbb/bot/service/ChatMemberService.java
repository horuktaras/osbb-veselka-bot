package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.ChatMember;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;
import ua.horuktaras.osbb.bot.repository.ChatMemberRepository;

import java.time.Instant;
import java.util.Optional;

@Service
public class ChatMemberService {

    private static final Logger log = LoggerFactory.getLogger(ChatMemberService.class);

    private final ChatMemberRepository chatMemberRepository;

    public ChatMemberService(ChatMemberRepository chatMemberRepository) {
        this.chatMemberRepository = chatMemberRepository;
    }

    @Transactional
    public ChatMember getOrCreate(Long chatId, Long telegramUserId) {
        return chatMemberRepository.findByChatIdAndTelegramUserId(chatId, telegramUserId)
                .orElseGet(() -> {
                    log.debug("Creating new ChatMember chatId={} userId={}", chatId, telegramUserId);
                    ChatMember member = new ChatMember();
                    member.setChatId(chatId);
                    member.setTelegramUserId(telegramUserId);
                    member.setVerificationStatus(VerificationStatus.PENDING);
                    return chatMemberRepository.save(member);
                });
    }

    @Transactional(readOnly = true)
    public Optional<ChatMember> findMember(Long chatId, Long telegramUserId) {
        return chatMemberRepository.findByChatIdAndTelegramUserId(chatId, telegramUserId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int addWarning(Long chatId, Long telegramUserId) {
        ChatMember member = chatMemberRepository
                .findByChatIdAndTelegramUserIdForUpdate(chatId, telegramUserId)
                .orElseGet(() -> {
                    ChatMember m = new ChatMember();
                    m.setChatId(chatId);
                    m.setTelegramUserId(telegramUserId);
                    m.setVerificationStatus(VerificationStatus.VERIFIED);
                    return chatMemberRepository.save(m);
                });
        member.setWarningCount(member.getWarningCount() + 1);
        chatMemberRepository.save(member);
        log.info("Warning added to userId={} in chatId={}, count={}", telegramUserId, chatId, member.getWarningCount());
        return member.getWarningCount();
    }

    @Transactional
    public void resetWarnings(Long chatId, Long telegramUserId) {
        chatMemberRepository.findByChatIdAndTelegramUserId(chatId, telegramUserId)
                .ifPresent(member -> {
                    member.setWarningCount(0);
                    chatMemberRepository.save(member);
                    log.info("Warnings reset for userId={} in chatId={}", telegramUserId, chatId);
                });
    }

    @Transactional
    public void mute(Long chatId, Long telegramUserId, Instant until) {
        ChatMember member = getOrCreate(chatId, telegramUserId);
        member.setMutedUntil(until);
        chatMemberRepository.save(member);
        log.info("User userId={} muted in chatId={} until={}", telegramUserId, chatId, until);
    }

    @Transactional
    public void unmute(Long chatId, Long telegramUserId) {
        chatMemberRepository.findByChatIdAndTelegramUserId(chatId, telegramUserId)
                .ifPresent(member -> {
                    member.setMutedUntil(null);
                    chatMemberRepository.save(member);
                    log.info("User userId={} unmuted in chatId={}", telegramUserId, chatId);
                });
    }

    @Transactional
    public void ban(Long chatId, Long telegramUserId) {
        ChatMember member = getOrCreate(chatId, telegramUserId);
        member.setBanned(true);
        chatMemberRepository.save(member);
        log.info("User userId={} banned in chatId={}", telegramUserId, chatId);
    }

    @Transactional
    public void unban(Long chatId, Long telegramUserId) {
        chatMemberRepository.findByChatIdAndTelegramUserId(chatId, telegramUserId)
                .ifPresent(member -> {
                    member.setBanned(false);
                    chatMemberRepository.save(member);
                    log.info("User userId={} unbanned in chatId={}", telegramUserId, chatId);
                });
    }

    @Transactional
    public void markVerified(Long chatId, Long telegramUserId) {
        ChatMember member = getOrCreate(chatId, telegramUserId);
        member.setVerificationStatus(VerificationStatus.VERIFIED);
        member.setVerifiedAt(Instant.now());
        chatMemberRepository.save(member);
        log.info("User userId={} verified in chatId={}", telegramUserId, chatId);
    }
}
