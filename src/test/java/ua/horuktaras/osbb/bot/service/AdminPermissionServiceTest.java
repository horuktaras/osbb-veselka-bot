package ua.horuktaras.osbb.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock
    private TelegramClient telegramClient;

    private AdminPermissionService adminPermissionService;

    @BeforeEach
    void setUp() {
        adminPermissionService = new AdminPermissionService();
    }

    private User buildUser(Long id, String firstName) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .isBot(false)
                .build();
    }

    @Test
    void returnsTrueWhenUserIsAdmin() throws TelegramApiException {
        Long chatId = 100L;
        Long adminUserId = 42L;

        ChatMemberAdministrator adminMember = mock(ChatMemberAdministrator.class);
        when(adminMember.getUser()).thenReturn(buildUser(adminUserId, "Admin"));

        ArrayList<ChatMember> admins = new ArrayList<>();
        admins.add(adminMember);

        when(telegramClient.execute(any(GetChatAdministrators.class))).thenReturn(admins);

        boolean result = adminPermissionService.isAdmin(chatId, adminUserId, telegramClient);

        assertTrue(result);
    }

    @Test
    void returnsFalseWhenUserIsNotAdmin() throws TelegramApiException {
        Long chatId = 100L;
        Long regularUserId = 99L;
        Long adminUserId = 42L;

        ChatMemberAdministrator adminMember = mock(ChatMemberAdministrator.class);
        when(adminMember.getUser()).thenReturn(buildUser(adminUserId, "Admin"));

        ArrayList<ChatMember> admins = new ArrayList<>();
        admins.add(adminMember);

        when(telegramClient.execute(any(GetChatAdministrators.class))).thenReturn(admins);

        boolean result = adminPermissionService.isAdmin(chatId, regularUserId, telegramClient);

        assertFalse(result);
    }

    @Test
    void returnsFalseWhenApiThrowsException() throws TelegramApiException {
        Long chatId = 100L;
        Long userId = 1L;

        when(telegramClient.execute(any(GetChatAdministrators.class)))
                .thenThrow(new TelegramApiException("Connection refused"));

        boolean result = adminPermissionService.isAdmin(chatId, userId, telegramClient);

        assertFalse(result);
    }

    @Test
    void cacheIsClearedByEvictCache() throws TelegramApiException {
        Long chatId = 100L;
        Long userId = 42L;

        ChatMemberAdministrator adminMember = mock(ChatMemberAdministrator.class);
        when(adminMember.getUser()).thenReturn(buildUser(userId, "Admin"));

        ArrayList<ChatMember> admins = new ArrayList<>();
        admins.add(adminMember);

        when(telegramClient.execute(any(GetChatAdministrators.class))).thenReturn(admins);

        // First call
        assertTrue(adminPermissionService.isAdmin(chatId, userId, telegramClient));
        // Evict cache
        adminPermissionService.evictCache(chatId, userId);
        // Second call should go to API again
        assertTrue(adminPermissionService.isAdmin(chatId, userId, telegramClient));

        verify(telegramClient, times(2)).execute(any(GetChatAdministrators.class));
    }
}
