package ua.horuktaras.osbb.bot.model.dto;

import ua.horuktaras.osbb.bot.model.enums.ModerationAction;

public record ModerationResult(
        boolean success,
        String message,
        ModerationAction action
) {
    public static ModerationResult success(ModerationAction action, String message) {
        return new ModerationResult(true, message, action);
    }

    public static ModerationResult failure(ModerationAction action, String message) {
        return new ModerationResult(false, message, action);
    }
}
