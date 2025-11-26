package io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands;

import java.util.HashMap;
import java.util.List;

import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramMessageKey;

public class Status extends MinecraftCommandBase {

    public Status() {
        super("status");
    }

    @Override
    public void execute(MinecraftUser sender, List<Object> args) {
        var platform = getBridge().getSocialPlatform(TelegramPlatform.class);
        MessageKey messageKey;
        switch (platform.getBotState()) {
            case Started:
                messageKey = TelegramMessageKey.BOT_STATUS_CONNECTED;
                break;
            case Starting:
                messageKey = TelegramMessageKey.BOT_STATUS_CONNECTING;
                break;
            case Stopped:
                messageKey = TelegramMessageKey.BOT_STATUS_STOPPED;
                break;
            case Stopping:
                messageKey = TelegramMessageKey.BOT_STATUS_STOPPING;
                break;
            default:
                throw new RuntimeException("Unexpected telegram bot state");

        }
        var msgTemplate = getBridge().getLocalizationService().getMessage(sender.getLocale(), messageKey);
        sender.sendMessage(msgTemplate, new HashMap<String, String>());
    }

}
