package ua.horuktaras.osbb.bot.exception;

public class VerificationException extends BotException {

    public VerificationException(String message) {
        super(message);
    }

    public VerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
