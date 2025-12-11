package io.github.kosyakmakc.socialBridgeTelegram.paper;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.kosyakmakc.socialBridge.SocialBridge;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.IModuleLoader;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramPlatform;
import io.github.kosyakmakc.socialBridgeTelegram.TelegramModule;

public class SocialBridgeTelegramPlugin extends JavaPlugin implements IModuleLoader {
    private final TelegramPlatform platform;
    private final TelegramModule module;
    
    public SocialBridgeTelegramPlugin() {
        platform = new TelegramPlatform();
        module = new TelegramModule(this);

        SocialBridge.INSTANCE.connectSocialPlatform(platform);
        SocialBridge.INSTANCE.connectModule(module);
    }

    @Override
    public void onDisable() {
        SocialBridge.INSTANCE.disconnectSocialPlatform(platform).join();
        SocialBridge.INSTANCE.disconnectModule(module).join();
    }
}
