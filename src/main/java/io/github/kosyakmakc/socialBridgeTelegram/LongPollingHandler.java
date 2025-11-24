package io.github.kosyakmakc.socialBridgeTelegram;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;

import java.io.StringReader;
import java.util.HashMap;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

public class LongPollingHandler implements LongPollingSingleThreadUpdateConsumer {
    private static final CommandArgument<String> systemWordArgument = CommandArgument.ofWord("/{botSuffix}-{commandLiteral} [arguments, ...]");
    private final TelegramPlatform socialPlatform;

    public LongPollingHandler(TelegramPlatform telegramPlatform) {
        this.socialPlatform = telegramPlatform;
    }

    @Override
    public void consume(Update update) {
        var socialUser = new TelegramUser(socialPlatform, update.getMessage());

        var message = update.getMessage().getText();

        if (message == null || message.isBlank()) {
            return;
        }

        // Commands handling
        if (TryCommandHandle(update, message, socialUser)) {
            return;
        }

        // TODO Messages handling in feature
//            var mcPlayer = socialUser.getMinecraftUser();
//
//            if (mcPlayer != null) {
//                chatEvent.getPlaceholders().addPlain(new Pair<>("authBridge-minecraftName", mcPlayer.getName()));
//            }
    }

    private boolean TryCommandHandle(Update chatEvent, String message, TelegramUser socialUser) {
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
            socialPlatform.sendMessage(socialUser, e.getMessage(), new HashMap<String, String>());
            return true;
        }
        return false;
    }
}
