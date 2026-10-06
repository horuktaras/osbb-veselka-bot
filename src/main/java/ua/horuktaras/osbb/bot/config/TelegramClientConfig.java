package ua.horuktaras.osbb.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class TelegramClientConfig {

    @Bean
    public TelegramClient telegramClient(BotProperties botProperties) {
        return new OkHttpTelegramClient(botProperties.token());
    }
}
