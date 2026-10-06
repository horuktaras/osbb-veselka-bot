package ua.horuktaras.osbb.bot.exception;

public class UserNotFoundException extends BotException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}
