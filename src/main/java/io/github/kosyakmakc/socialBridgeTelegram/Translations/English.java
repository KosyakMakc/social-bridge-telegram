package io.github.kosyakmakc.socialBridgeTelegram.Translations;

import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramMessageKey;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.LocalizationRecord;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;

import java.util.List;

public class English implements ITranslationSource {

    @Override
    public String getLanguage() {
        return LocalizationService.defaultLocale;
    }

    @Override
    public List<LocalizationRecord> getRecords() {
        return List.of(
                new LocalizationRecord(TelegramMessageKey.SET_TOKEN_SUCCESS.key(), "<green>New token saved and applied.</green>"),
                new LocalizationRecord(TelegramMessageKey.SET_TOKEN_FAILED_CONFIG.key(), "<red>Failed to save token to configuration service.</red>"),
                new LocalizationRecord(TelegramMessageKey.SET_TOKEN_FAILED_STOP_BOT.key(), "<red>Failed to stop telegram bot, new token saved, but not applied.</red>"),
                new LocalizationRecord(TelegramMessageKey.SET_TOKEN_FAILED_START_BOT.key(), "<red>Failed to start telegram bot, new token saved, but not applied.</red>"),

                new LocalizationRecord(TelegramMessageKey.BOT_STATUS_CONNECTING.key(), "<yellow>Telegram bot are connecting...</yellow>"),
                new LocalizationRecord(TelegramMessageKey.BOT_STATUS_CONNECTED.key(), "<green>Telegram bot successfully connected.</green>"),
                new LocalizationRecord(TelegramMessageKey.BOT_STATUS_STOPPING.key(), "<yellow>Telegram bot are stopping...</yellow>"),
                new LocalizationRecord(TelegramMessageKey.BOT_STATUS_STOPPED.key(), "<red>Telegram bot stopped.</red>")

                // new LocalizationRecord(getLanguage(), TelegramMessageKey.BOT_STARTED.key(), "✅ Social bot connected!"),
                // new LocalizationRecord(getLanguage(), TelegramMessageKey.BOT_STOPPED.key(), "❌ Social bot stopped!")
        );
    }

}
