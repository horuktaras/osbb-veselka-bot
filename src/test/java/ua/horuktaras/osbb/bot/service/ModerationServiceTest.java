package ua.horuktaras.osbb.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.ModerationResult;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.model.enums.WarningPunishment;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private ChatMemberService chatMemberService;
    @Mock private ChatConfigService chatConfigService;
    @Mock private ModerationLogService moderationLogService;
    @Mock private TelegramApiService telegramApiService;
    @Mock private TelegramClient telegramClient;

    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService(
                chatMemberService, chatConfigService, moderationLogService, telegramApiService);
    }

    @Test
    void warnUserIncrementsCount() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;

        ChatConfig config = new ChatConfig();
        config.setWarningLimit(3);
        config.setWarningPunishment(WarningPunishment.MUTE);

        when(chatConfigService.getOrCreate(chatId)).thenReturn(config);
        when(chatMemberService.addWarning(chatId, targetId)).thenReturn(1);

        ModerationResult result = moderationService.warnUser(chatId, adminId, targetId, "test", telegramClient);

        assertTrue(result.success());
        assertEquals(ModerationAction.WARN, result.action());
        verify(chatMemberService).addWarning(chatId, targetId);
        verify(moderationLogService).log(eq(chatId), eq(adminId), eq(targetId), eq(ModerationAction.WARN), any(), any());
    }

    @Test
    void warnUserAtLimitTriggersMute() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;

        ChatConfig config = new ChatConfig();
        config.setWarningLimit(3);
        config.setWarningPunishment(WarningPunishment.MUTE);
        config.setWarningPunishmentDurationSeconds(3600);

        when(chatConfigService.getOrCreate(chatId)).thenReturn(config);
        when(chatMemberService.addWarning(chatId, targetId)).thenReturn(3); // at limit
        when(telegramApiService.restrictUser(eq(chatId), eq(targetId), any(ChatPermissions.class))).thenReturn(true);

        ModerationResult result = moderationService.warnUser(chatId, adminId, targetId, "too many warns", telegramClient);

        assertTrue(result.success());
        verify(telegramApiService).restrictUser(eq(chatId), eq(targetId), any(ChatPermissions.class));
        verify(chatMemberService).mute(eq(chatId), eq(targetId), any());
    }

    @Test
    void warnUserAtLimitTriggersBan() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;

        ChatConfig config = new ChatConfig();
        config.setWarningLimit(3);
        config.setWarningPunishment(WarningPunishment.BAN);

        when(chatConfigService.getOrCreate(chatId)).thenReturn(config);
        when(chatMemberService.addWarning(chatId, targetId)).thenReturn(3);
        when(telegramApiService.banUser(eq(chatId), eq(targetId), isNull())).thenReturn(true);

        ModerationResult result = moderationService.warnUser(chatId, adminId, targetId, "auto ban", telegramClient);

        assertTrue(result.success());
        verify(telegramApiService).banUser(chatId, targetId, null);
        verify(chatMemberService).ban(chatId, targetId);
    }

    @Test
    void muteUserSuccess() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;
        Duration duration = Duration.ofHours(1);

        when(telegramApiService.restrictUser(eq(chatId), eq(targetId), any(ChatPermissions.class))).thenReturn(true);

        ModerationResult result = moderationService.muteUser(chatId, adminId, targetId, duration, "spam", telegramClient);

        assertTrue(result.success());
        assertEquals(ModerationAction.MUTE, result.action());
        verify(chatMemberService).mute(eq(chatId), eq(targetId), any());
    }

    @Test
    void muteUserFailureWhenApiCallFails() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;

        when(telegramApiService.restrictUser(eq(chatId), eq(targetId), any(ChatPermissions.class))).thenReturn(false);

        ModerationResult result = moderationService.muteUser(chatId, adminId, targetId, Duration.ofHours(1), null, telegramClient);

        assertFalse(result.success());
        assertEquals(ModerationAction.MUTE, result.action());
        verify(chatMemberService, never()).mute(any(), any(), any());
    }

    @Test
    void unmuteUserSuccess() {
        Long chatId = 100L;
        Long adminId = 1L;
        Long targetId = 2L;

        when(telegramApiService.unrestrictUser(chatId, targetId)).thenReturn(true);

        ModerationResult result = moderationService.unmuteUser(chatId, adminId, targetId, null, telegramClient);

        assertTrue(result.success());
        assertEquals(ModerationAction.UNMUTE, result.action());
        verify(chatMemberService).unmute(chatId, targetId);
    }
}
