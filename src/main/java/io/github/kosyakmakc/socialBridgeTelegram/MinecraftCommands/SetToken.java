package io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;

public class SetToken extends MinecraftCommandBase{

    public SetToken() {
        super("setupToken", List.of(CommandArgument.ofWord("Telegram token")));
    }

    @Override
    public void execute(MinecraftUser sender, List<Object> parameters) {
        var token = (String) parameters.get(0);
        var placeholders = new HashMap<String, String>();
        if (validateToken(token)) {
            this.getBridge().getSocialPlatform(TelegramPlatform.class).setupToken(token);
            sender.sendMessage(token, placeholders);
        }
        else {
            sender.sendMessage(token, placeholders);
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

        var rawBotToken = words[1];
        if (rawBotToken.length() == 0) {
            return false;
        }

        try {
            Integer.parseInt(rawBotId);
        }
        catch (NumberFormatException err) {
            return false;
        }

        return true;
    }
}
