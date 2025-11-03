package io.github.kosyakmakc.socialBridgeTelegram;
import dev.vanutp.tgbridge.common.TgbridgeEvents;
import dev.vanutp.tgbridge.common.compat.ITgbridgeCompat;
import dev.vanutp.tgbridge.common.models.TgbridgeTgChatMessageEvent;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;

import java.io.StringReader;
import java.util.Objects;

public class MessageHandlerIntegration implements ITgbridgeCompat {
    private static final CommandArgument<String> systemWordArgument = CommandArgument.ofWord("/{botSuffix}-{commandLiteral} [arguments, ...]");
    private final TelegramPlatform socialPlatform;

    public MessageHandlerIntegration(TelegramPlatform telegramPlatform) {
        this.socialPlatform = telegramPlatform;
    }

    @Override
    public boolean shouldEnable() {
        return true;
    }

    @Override
    public void enable() {
        TgbridgeEvents.INSTANCE.getTG_CHAT_MESSAGE().addListener((chatEvent) -> {
            var socialUser = new TelegramUser(socialPlatform, chatEvent.getMessage().getFrom());

            var message = chatEvent.getMessage().getEffectiveText();

            if (message == null || message.isBlank()) {
                return;
            }

            // Commands handling
            if (TryCommandHandle(chatEvent, message, socialUser)) {
                chatEvent.setCancelled(true);
                return;
            }

            // TODO Messages handling
//            var mcPlayer = socialUser.getMinecraftUser();
//
//            if (mcPlayer != null) {
//                chatEvent.getPlaceholders().addPlain(new Pair<>("authBridge-minecraftName", mcPlayer.getName()));
//            }

        });
    }

    private boolean TryCommandHandle(TgbridgeTgChatMessageEvent chatEvent, String message, TelegramUser socialUser) {
        var argsReader = new StringReader(message);

        try {
            // pumping "/{moduleSuffix}-{commandLiteral}" in reader
            var commandLiteral = systemWordArgument.getValue(argsReader);

            for (var module : socialPlatform.getBridge().getModules()) {

                if (!commandLiteral.startsWith('/' + module.getName())) {
                    continue;
                }

                for (var socialCommand : module.getSocialCommands()) {
                    if (commandLiteral.equals('/' + module.getName() + '-' + socialCommand.getLiteral())) {
                        socialCommand.handle(socialUser, argsReader);
                        return true;
                    }
                }
            }
        } catch (ArgumentFormatException e) {
            e.logTo(socialPlatform.getBridge().getLogger());
            // TODO reply error to user in social platform
//            var chatId = chatEvent.getMessage().getChat().getId();
//            var replyToId = chatEvent.getMessage().getMessageId();
//            socialPlatform.getTgBridge().getBot().sendMessage(
//                    chatId,
//                    e.getMessage(),
//                    null,
//                    replyToId,
//                    "HTML",
//                    true,
//                    null);

            return true;
        }
        return false;
    }
}
