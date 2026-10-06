package ua.horuktaras.osbb.bot.util;

import org.telegram.telegrambots.meta.api.objects.User;

public final class UserUtils {

    private UserUtils() {}

    public static String getDisplayName(User user) {
        if (user == null) return "Unknown";
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            sb.append(user.getFirstName());
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getLastName());
        }
        if (sb.isEmpty() && user.getUserName() != null) {
            sb.append("@").append(user.getUserName());
        }
        if (sb.isEmpty()) {
            sb.append("User ").append(user.getId());
        }
        return sb.toString();
    }

    public static Long getUserId(User user) {
        if (user == null) return null;
        return user.getId();
    }
}
