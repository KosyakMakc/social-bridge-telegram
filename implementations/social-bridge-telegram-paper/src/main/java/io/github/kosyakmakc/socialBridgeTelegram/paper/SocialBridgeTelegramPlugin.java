package io.github.kosyakmakc.socialBridgeTelegram.paper;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.kosyakmakc.socialBridge.SocialBridge;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramModule;

public class SocialBridgeTelegramPlugin extends JavaPlugin {
    public SocialBridgeTelegramPlugin() {
        SocialBridge.INSTANCE.registerSocialPlatform(new TelegramPlatform());
        SocialBridge.INSTANCE.registerModule(new TelegramModule());
    }
}
