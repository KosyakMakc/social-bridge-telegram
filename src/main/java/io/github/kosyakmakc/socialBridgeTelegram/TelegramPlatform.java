package io.github.kosyakmakc.socialBridgeTelegram;
import dev.vanutp.tgbridge.common.TelegramBridge;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;

import java.lang.Runtime.Version;
import java.util.HashMap;

public class TelegramPlatform implements ISocialPlatform {
    private ISocialBridge bridge;
    private TelegramBridge tgBridge;

    private final Version socialBridgeCompabilityVersion = Version.parse("0.1.0");

    @Override
    public void Start() {
        this.tgBridge = TelegramBridge.Companion.getINSTANCE();

        var self = this;

        tgBridge.addIntegration(new MessageHandlerIntegration(this));
//        for (var socialCommand : bridge.getSocialCommands()) {
//            tgBridge.getBot().registerCommandHandler(socialCommand.getLiteral(), (tgMessage, continuation) -> {
//
//                return null;
//            });
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
    public void sendMessage(SocialUser telegramUser, String message, HashMap<String, String> placeholders) {

//        var builder = MiniMessage.builder()
//                .tags(TagResolver.builder()
//                        .resolver(StandardTags.defaults())
//                        .build());
//
//        for (var placeholderKey : placeholders.keySet()) {
//            builder.editTags(x -> x.resolver(Placeholder.component(placeholderKey, Component.text(placeholders.get(placeholderKey)))));
//        }
//        var builtMessage = builder.build().deserialize(message);


        // TODO api extend?
//            var chatId = chatEvent.getMessage().getChat().getId();
//            var replyToId = chatEvent.getMessage().getMessageId();
//            socialPlatform.getTgBridge().getBot().sendMessage(
//                    chatId,
//                    builtMessage,
//                    null,
//                    replyToId,
//                    "HTML",
//                    true,
//                    null);
//        this.getBridge().getLogger().info("tgMessage to \"" + telegramUser.getName() + "\" - " + builtMessage);
        this.getBridge().getLogger().info("tgMessage to \"" + telegramUser.getName() + "\" - " + message);
    }

    @Override
    public Version getCompabilityVersion() {
        return socialBridgeCompabilityVersion;
    }
}
