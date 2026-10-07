package ua.horuktaras.osbb.bot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "telegram.bot")
@Validated
public record BotProperties(
        @NotBlank String token,
        @NotBlank String username,
        @NotBlank String adminChatId
) {}
