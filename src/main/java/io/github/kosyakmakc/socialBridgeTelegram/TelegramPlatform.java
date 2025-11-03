package io.github.kosyakmakc.socialBridgeTelegram;
import dev.vanutp.tgbridge.common.TelegramBridge;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;

import java.util.HashMap;

public class TelegramPlatform implements ISocialPlatform {
    private ISocialBridge bridge;
    private TelegramBridge tgBridge;

    private final Version socialBridgeCompabilityVersion = new Version("0.2.0");

    @Override
    public void Start() {
        this.tgBridge = TelegramBridge.Companion.getINSTANCE();

        tgBridge.addIntegration(new MessageHandlerIntegration(this));

        // commands handling
//        for (var module : bridge.getModules()) {
//            for (var socialCommand : module.getSocialCommands()) {
//                tgBridge.getBot().registerCommandHandler(module.getName() + '-' + socialCommand.getLiteral(), (tgMessage, continuation) -> {
//                    // TODO надо бы регистрировать команды КАК КОМАНДЫ...
//                    // А не как сейчас в tgbridge в виде обработки сообщений)
//                    // Тем не менее моя текущая реализация тоже работает через обработку сообщений...
//                    return null;
//                });
//            }
//        }
    }

    @Override
    public void setAuthBridge(ISocialBridge bridge) {
        this.bridge = bridge;
    }

    public ISocialBridge getBridge() {
        return bridge;
    }

    public TelegramBridge getTgBridge() {
        return tgBridge;
    }

    @Override
    public String getPlatformName() {
        return "Telegram";
    }

    @Override
    public void sendMessage(SocialUser socialUser, String message, HashMap<String, String> placeholders) {
//        var builder = MiniMessage.builder()
//                .tags(TagResolver.builder()
//                        .resolver(StandardTags.defaults())
//                        .build());
//
//        for (var placeholderKey : placeholders.keySet()) {
//            builder.editTags(x -> x.resolver(Placeholder.component(placeholderKey, Component.text(placeholders.get(placeholderKey)))));
//        }
//        var builtMessage = builder.build().deserialize(message);


        // TODO вызов "getTgBridge().getBot().sendMessage" дает краш "java.lang.NoSuchMethodError"
//            var tgUser = (TelegramUser) socialUser;
//            var chatId = tgUser.getLastMessage().getChat().getId();
//            var replyToId = tgUser.getLastMessage().getMessageId();
//            getTgBridge().getBot().sendMessage(
//                    chatId,
//                    "test message",
//                    null,
//                    replyToId,
//                    "HTML",
//                    true,
//                    null);
//        this.getBridge().getLogger().info("tgMessage to \"" + socialUser.getName() + "\" - " + builtMessage);
        this.getBridge().getLogger().info("tgMessage to \"" + socialUser.getName() + "\" - " + message);
    }

    @Override
    public Version getCompabilityVersion() {
        return socialBridgeCompabilityVersion;
    }
}
