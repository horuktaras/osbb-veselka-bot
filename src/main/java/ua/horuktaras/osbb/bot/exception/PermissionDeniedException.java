package ua.horuktaras.osbb.bot.exception;

public class PermissionDeniedException extends BotException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(Long userId, Long chatId) {
        super("User " + userId + " does not have permission in chat " + chatId);
    }
}
