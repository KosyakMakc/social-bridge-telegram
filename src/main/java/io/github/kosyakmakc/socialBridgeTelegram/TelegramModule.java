package io.github.kosyakmakc.socialBridgeTelegram;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.ISocialModule;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.IMinecraftCommand;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.DefaultTranslations.ITranslationSource;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.IModuleLoader;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands.SetToken;
import io.github.kosyakmakc.socialBridgeTelegram.MinecraftCommands.Status;
import io.github.kosyakmakc.socialBridgeTelegram.Translations.English;
import io.github.kosyakmakc.socialBridgeTelegram.Translations.Russian;

public class TelegramModule implements ISocialModule {
    public static UUID MODULE_ID = UUID.fromString("f7e27e90-3e6c-4331-990f-1977b8a5481a");
    private static final Version compabilityVersion = new Version("0.5.0");
    private static final String ModuleName = "telegram";

    private final List<IMinecraftCommand> minecraftCommands = List.of(
        new SetToken(),
        new Status()
    );

    private final List<ISocialCommand> socialCommands = List.of();

    private final List<ITranslationSource> translationSources = List.of(
        new English(),
        new Russian()
    );

    private final IModuleLoader loader;

    private ISocialBridge socialBridge;

    public TelegramModule(IModuleLoader loader) {
        this.loader = loader;
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
    public CompletableFuture<Boolean> disable() {
        this.socialBridge = null;
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> enable(ISocialBridge socialBridge) {
        this.socialBridge = socialBridge;

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public ISocialBridge getBridge() {
        return socialBridge;
    }

    @Override
    public UUID getId() {
        return MODULE_ID;
    }

    @Override
    public IModuleLoader getLoader() {
        return loader;
    }

}
