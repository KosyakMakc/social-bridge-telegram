package io.github.kosyakmakc.socialBridgeTelegram;
import dev.vanutp.tgbridge.common.TgMessage;
import dev.vanutp.tgbridge.common.TgUser;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUserIdType;

import java.util.HashMap;

public class TelegramUser extends SocialUser {
    private final TgUser user;
    private final TgMessage lastMessage;

    public TelegramUser(ISocialPlatform socialPlatform, TgMessage message) {
        super(socialPlatform);
        this.lastMessage = message;
        this.user = message.getFrom();
    }

    @Override
    public String getName() {
        return user.getFullName();
    }

    @Override
    public void sendMessage(String message, HashMap<String, String> placeholders) {
        getPlatform().sendMessage(this, message, placeholders);
    }

    @Override
    public String getLocale() {
        // TODO api extend?
        return LocalizationService.defaultLocale;
    }

    @Override
    public SocialUserIdType getIdType() {
        return SocialUserIdType.Long;
    }

    @Override
    public Object getId() {
        return user.getId();
    }

    public TgMessage getLastMessage() {
        return lastMessage;
    }
}
