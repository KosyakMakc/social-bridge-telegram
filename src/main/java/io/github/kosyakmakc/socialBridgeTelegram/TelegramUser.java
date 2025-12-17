package io.github.kosyakmakc.socialBridgeTelegram;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.IdentifierType;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridgeTelegram.DatabaseTables.TelegramUserTable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

public class TelegramUser extends SocialUser implements Comparable<TelegramUser> {
    private final TelegramUserTable userRecord;
    private final Identifier id;
    
    private Message lastMessage;

    public TelegramUser(ISocialPlatform socialPlatform, TelegramUserTable userRecord) {
        super(socialPlatform);
        this.userRecord = userRecord;
        this.id = new Identifier(IdentifierType.Long, userRecord.getId());
    }

    @Override
    public String getName() {
        var lastname = userRecord.getLastName();
        if (lastname == null) {
            return userRecord.getFirstName();
        }
        else {
            return userRecord.getFirstName() + ' ' + userRecord.getLastName();
        }
    }

    @Override
    public void sendMessage(String message, HashMap<String, String> placeholders) {
        getPlatform().sendMessage(this, message, placeholders);
    }

    @Override
    public String getLocale() {
        var userLocale = userRecord.getLocalization();
        return userLocale == null ? LocalizationService.defaultLocale : userLocale;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    @Override
    public int compareTo(TelegramUser anotherUser) {
        var delta = (long) this.getId().value() - (long) anotherUser.getId().value();
        return delta > 0
            ? 1
            : delta < 0
                ? -1
                : 0;
    }

    public TelegramUserTable getUserRecord() {
        return userRecord;
    }

    public boolean TryActualize(User tgUser) {
        var changed = false;

        if (userRecord.getUsername() != tgUser.getUserName()) {
            userRecord.setUsername(tgUser.getUserName());
            changed = true;
        }
        if (userRecord.getFirstName() != tgUser.getFirstName()) {
            userRecord.setFirstName(tgUser.getFirstName());
            changed = true;
        }
        if (userRecord.getLastName() != tgUser.getLastName()) {
            userRecord.setLastName(tgUser.getLastName());
            changed = true;
        }
        if (userRecord.getLocalization() != tgUser.getLanguageCode()) {
            userRecord.setLocalization(tgUser.getLanguageCode());
            changed = true;
        }

        if (changed) {
            userRecord.setUpdatedAt(Date.from(Instant.now()));
            
            // non-blocking save user in background
            ((TelegramPlatform) getPlatform()).getBridge().queryDatabase(ctx -> {
                var table = ctx.getDaoTable(TelegramUserTable.class);
                try {
                    table.update(userRecord);
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }

        return changed;
    }
}
