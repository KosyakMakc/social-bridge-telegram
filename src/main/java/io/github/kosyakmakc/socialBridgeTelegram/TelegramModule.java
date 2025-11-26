package io.github.kosyakmakc.socialBridgeTelegram;

import java.util.List;

import io.github.kosyakmakc.socialBridge.IBridgeModule;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.IMinecraftCommand;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands.SetToken;
import io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands.Status;
import io.github.kosyakmakc.socialBridgeTelegram.Translations.English;

public class TelegramModule implements IBridgeModule {
    private static final Version compabilityVersion = new Version("0.2.1");
    private static final String ModuleName = "Telegram";

    private final List<IMinecraftCommand> minecraftCommands = List.of(
        new SetToken(),
        new Status()
    );

    private final List<ISocialCommand> socialCommands = List.of();

    private final List<ITranslationSource> translationSources = List.of(
        new English()
    );

    private ISocialBridge socialBridge;

    public TelegramModule() {

    }

    @Override
    public Version getCompabilityVersion() {
        return compabilityVersion;
    }

    @Override
    public List<IMinecraftCommand> getMinecraftCommands() {
        return minecraftCommands;
    }

    @Override
    public String getName() {
        return ModuleName;
    }

    @Override
    public List<ISocialCommand> getSocialCommands() {
        return socialCommands;
    }

    @Override
    public List<ITranslationSource> getTranslations() {
        return translationSources;
    }

    @Override
    public boolean init(ISocialBridge socialBridge) {
        this.socialBridge = socialBridge;
        for (var minecraftCommand : minecraftCommands) {
            minecraftCommand.init(socialBridge);
        }
        
        for (var socialCommand : socialCommands) {
            socialCommand.init(socialBridge);
        }
        return true;
    }

}
