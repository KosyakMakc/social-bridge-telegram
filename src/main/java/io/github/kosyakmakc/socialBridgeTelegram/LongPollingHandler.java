package io.github.kosyakmakc.socialBridgeTelegram;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.IdentifierType;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridgeTelegram.DatabaseTables.TelegramUserTable;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.HashMap;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

public class LongPollingHandler implements LongPollingSingleThreadUpdateConsumer {
    private static final CommandArgument<String> systemWordArgument = CommandArgument.ofWord("/{botSuffix}-{commandLiteral} [arguments, ...]");
    private final TelegramPlatform socialPlatform;

    private String botUsername;

    public LongPollingHandler(TelegramPlatform telegramPlatform) {
        this.socialPlatform = telegramPlatform;
    }

    @Override
    public void consume(Update update) {
        var tgMessage = update.getMessage();
        if (tgMessage == null) {
            return;
        }

        var tgUser = tgMessage.getFrom();
        var longId = tgUser.getId();
        var identifier = new Identifier(IdentifierType.Long, longId);
        socialPlatform.tryGetUser(identifier)
            .thenApply(socialUser -> {
                if (socialUser == null) {
                    var dbUser = new TelegramUserTable(longId, tgUser.getUserName(), tgUser.getFirstName(), tgUser.getLastName(), tgUser.getLanguageCode());
                    socialUser = new TelegramUser(socialPlatform, dbUser);

                    // non-blocking save user in background
                    socialPlatform.getBridge().queryDatabase(ctx -> {
                        var table = ctx.getDaoTable(TelegramUserTable.class);
                        try {
                            table.createIfNotExists(dbUser);
                            return true;
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return false;
                        }
                    });
                }
                return socialUser;
            })
            .thenApply(socialUser -> {
                if (socialUser instanceof TelegramUser telegramUser) {
                    telegramUser.setLastMessage(tgMessage);

                    var isChanged = telegramUser.TryActualize(tgUser);
                    if (isChanged) {
                        socialPlatform.getLogger().info("telegram user info updated (id " + longId + " - " + telegramUser.getName() + ")");
                    }
                }

                return socialUser;
            })
            .thenAcceptAsync(socialUser -> {
                var message = update.getMessage().getText();
                
                if (message == null || message.isBlank()) {
                    return;
                }

                // Commands handling
                if (TryCommandHandle(update, message, socialUser)) {
                    return;
                }
                
                // TODO Messages handling in future
    //            var mcPlayer = socialUser.getMinecraftUser();
    //
    //            if (mcPlayer != null) {
        //                chatEvent.getPlaceholders().addPlain(new Pair<>("authBridge-minecraftName", mcPlayer.getName()));
        //            }
            });
    }

    private boolean TryCommandHandle(Update chatEvent, String message, SocialUser socialUser) {
        var argsReader = new StringReader(message);

        try {
            // pumping "/{moduleSuffix}-{commandLiteral}" in reader
            var commandLiteral = systemWordArgument.getValue(argsReader);

            if (commandLiteral.endsWith('@' + botUsername)) {
                commandLiteral = commandLiteral.substring(0, commandLiteral.length() - botUsername.length() - 1);
            }

            for (var module : socialPlatform.getBridge().getModules()) {

                if (!commandLiteral.startsWith('/' + module.getName())) {
                    continue;
                }

                for (var socialCommand : module.getSocialCommands()) {
                    if (commandLiteral.equals('/' + module.getName() + '_' + socialCommand.getLiteral())) {
                        socialCommand.handle(socialUser, argsReader);
                        return true;
                    }
                }
            }
        } catch (ArgumentFormatException e) {
            e.logTo(socialPlatform.getBridge().getLogger());
            socialPlatform.sendMessage(socialUser, e.getMessage(), new HashMap<String, String>());
            return true;
        }
        return false;
    }

    public void setBotUsername(String userName) {
        botUsername = userName;
    }
}
