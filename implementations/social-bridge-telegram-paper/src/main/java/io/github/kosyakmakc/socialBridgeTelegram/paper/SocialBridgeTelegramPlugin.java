package io.github.kosyakmakc.socialBridgeTelegram.paper;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.kosyakmakc.socialBridge.SocialBridge;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramModule;

public class SocialBridgeTelegramPlugin extends JavaPlugin {
    private final TelegramPlatform platform;
    public SocialBridgeTelegramPlugin() {
        platform = new TelegramPlatform();
        SocialBridge.INSTANCE.registerSocialPlatform(platform);
        SocialBridge.INSTANCE.registerModule(new TelegramModule());
    }

    @Override
    public void onDisable() {
        platform.stop().join();
    }
}
