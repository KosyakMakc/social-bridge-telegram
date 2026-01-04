package io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramModule;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramMessageKey;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramPermissions;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TranslationException;

public class SetToken extends MinecraftCommandBase {

    public SetToken() {
        super("setupToken", TelegramMessageKey.SET_TOKEN_DESCRIPTION, TelegramPermissions.CAN_SET_LOGIN, List.of(CommandArgument.ofGreedyString("Telegram token")));
    }

    @Override
    public void execute(MinecraftUser sender, List<Object> parameters) {
        var module = getBridge().getModule(TelegramModule.class);
        var token = (String) parameters.get(0);
        var placeholders = new HashMap<String, String>();
        if (validateToken(token)) {
            var setupTask = this.getBridge().getSocialPlatform(TelegramPlatform.class).setupToken(token);

            setupTask
                .thenCompose(isSuccess ->
                    getBridge().getLocalizationService().getMessage(module, sender.getLocale(), TelegramMessageKey.SET_TOKEN_SUCCESS))
                .thenAccept(msgTemplate ->
                    sender.sendMessage(msgTemplate, placeholders));

            setupTask
                .exceptionally(err -> {
                    CompletableFuture<String> task;
                    if (err instanceof TranslationException translationException) {
                        task = getBridge().getLocalizationService().getMessage(module, sender.getLocale(), translationException.getMessageKey());
                    } else {
                        task = CompletableFuture.completedFuture(err.getMessage());
                    }

                    task.thenAccept(msgTemplate -> {
                        sender.sendMessage(msgTemplate, placeholders);
                    });
                    
                    return true; // not used, just for close signature of lambda
                });
        }
        else {
            sender.sendMessage("please provide valid token (123456:secret_token)", placeholders);
        }
    }

    private boolean validateToken(String token) {
        var words = token.split(":");
        if (words.length != 2) {
            return false;
        }

        var rawBotId = words[0];
        if (rawBotId.length() == 0) {
            return false;
        }

        try {
            Long.parseLong(rawBotId);
        }
        catch (NumberFormatException err) {
            return false;
        }

        var rawBotToken = words[1];
        if (!rawBotToken.matches("^[a-zA-Z0-9_-]{35}$")) {
            return false;
        }

        return true;
    }
}
