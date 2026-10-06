package ua.horuktaras.osbb.bot.verification;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

@Component
public class VerificationKeyboardFactory {

    public InlineKeyboardMarkup createVerificationKeyboard(Long chatId, Long userId) {
        InlineKeyboardButton agreeButton = InlineKeyboardButton.builder()
                .text("\u2705 I agree to the rules")
                .callbackData("verify:agree:" + chatId + ":" + userId)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow(agreeButton);
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }
}
