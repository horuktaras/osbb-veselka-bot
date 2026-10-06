package ua.horuktaras.osbb.bot.model.dto;

import org.telegram.telegrambots.meta.api.objects.User;

public record UserInfo(
        Long telegramUserId,
        String username,
        String firstName,
        String lastName,
        String displayName
) {
    public static UserInfo from(User user) {
        if (user == null) {
            return null;
        }
        String display = user.getFirstName() != null ? user.getFirstName() : "";
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            display = display + " " + user.getLastName();
        }
        if (display.isBlank() && user.getUserName() != null) {
            display = "@" + user.getUserName();
        }
        if (display.isBlank()) {
            display = "User " + user.getId();
        }
        return new UserInfo(
                user.getId(),
                user.getUserName(),
                user.getFirstName(),
                user.getLastName(),
                display.trim()
        );
    }
}
