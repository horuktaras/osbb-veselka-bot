package ua.horuktaras.osbb.bot.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Interface for all bot command handlers.
 */
public interface BotCommand {

    /**
     * @return the command name without slash, e.g. "help"
     */
    String getCommand();

    /**
     * @return short description of this command
     */
    String getDescription();

    /**
     * Whether this command requires the calling user to be an admin.
     */
    boolean requiresAdmin();

    /**
     * Handle the command.
     */
    void handle(Update update);
}
