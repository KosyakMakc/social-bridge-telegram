package io.github.kosyakmakc.socialBridgeTelegram;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUserIdType;

import java.util.HashMap;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public class TelegramUser extends SocialUser {
    private final User user;
    private final Message lastMessage;

    public TelegramUser(ISocialPlatform socialPlatform, Message message) {
        super(socialPlatform);
        this.lastMessage = message;
        this.user = message.getFrom();
    }

    @Override
    public String getName() {
        var lastname = user.getLastName();
        if (lastname == null) {
            return user.getFirstName();
        }
        else {
            return user.getFirstName() + ' ' + user.getLastName();
        }
    }

    @Override
    public void sendMessage(String message, HashMap<String, String> placeholders) {
        getPlatform().sendMessage(this, message, placeholders);
    }

    @Override
    public String getLocale() {
        var userLocale = user.getLanguageCode();
        return userLocale == null ? LocalizationService.defaultLocale : userLocale;
    }

    @Override
    public SocialUserIdType getIdType() {
        return SocialUserIdType.Long;
    }

    @Override
    public Object getId() {
        return user.getId();
    }

    public Message getLastMessage() {
        return lastMessage;
    }
}
