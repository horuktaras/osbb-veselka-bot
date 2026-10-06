package ua.horuktaras.osbb.bot.util;

import java.time.Duration;

public final class MessageFormatter {

    private MessageFormatter() {}

    public static String formatUserMention(Long userId, String name) {
        return "<a href=\"tg://user?id=" + userId + "\">" + escapeHtml(name) + "</a>";
    }

    public static String formatWarningMessage(int count, int limit) {
        return "⚠️ <b>Warning!</b> This user has received " + count + "/" + limit + " warnings.";
    }

    public static String formatMuteMessage(String username, Duration duration) {
        if (duration == null || duration.isZero()) {
            return "🔇 User <b>" + escapeHtml(username) + "</b> has been muted permanently.";
        }
        return "🔇 User <b>" + escapeHtml(username) + "</b> has been muted for " + formatDuration(duration) + ".";
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "permanently";
        }
        long days = duration.toDays();
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
        return duration.getSeconds() + " second(s)";
    }

    public static String bold(String text) {
        return "<b>" + escapeHtml(text) + "</b>";
    }

    public static String code(String text) {
        return "<code>" + escapeHtml(text) + "</code>";
    }
}
