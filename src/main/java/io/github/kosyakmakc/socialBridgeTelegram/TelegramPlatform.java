package io.github.kosyakmakc.socialBridgeTelegram;

import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramMessageKey;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TranslationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class TelegramPlatform implements ISocialPlatform {
    private static final String configurationPath = "social-bridge-telegram";
    private static final String configurationPathToken = configurationPath + "_token";
    private static final String configurationPathRetryMax = configurationPath + "_retries-max";
    private static final int defaultRetryMax = 10;
    private static final String configurationPathRetryDelay = configurationPath + "_retries-delay";
    private static final int defaultRetryDelay = 2;

    private final Version socialBridgeCompabilityVersion = new Version("0.2.1");
    private final LongPollingHandler TgUpdatesHandler = new LongPollingHandler(this);
    private final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
    
    private TelegramClient telegramClient;
    private BotState botState = BotState.Stopped;

    private ISocialBridge bridge;

    @Override
    public void Start() {
        start();
    }

    private CompletableFuture<Boolean> start() {
        if (botState != BotState.Stopped) {
            return CompletableFuture.completedFuture(false);
        }

        getBridge().getLogger().info("Telegram bot starting...");

        botState = BotState.Starting;
        var token = getTgToken();

        if (token.isBlank()) {
            getBridge().getLogger().info("Token missed, connect to telegram canceled");
            botState = BotState.Stopped;
            return CompletableFuture.completedFuture(false);
        }

        return withRetries(() -> {
            try {
                botsApplication.registerBot(token, TgUpdatesHandler);
                telegramClient = new OkHttpTelegramClient(token);
                botState = BotState.Started;
                getBridge().getLogger().info("Telegram bot connected");
            } catch (TelegramApiException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });
    }

    public CompletableFuture<Boolean> stop() {
        if (botState != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        getBridge().getLogger().info("Telegram bot stopping...");

        botState = BotState.Stopping;
        var token = getTgToken();
        
        return withRetries(() -> {
            try {
                botsApplication.unregisterBot(token);
                telegramClient = null;
                botState = BotState.Stopped;
                getBridge().getLogger().info("Telegram bot stopped");
            }
            catch (TelegramApiException err) {
                err.printStackTrace();
                return false;
            }
            return true;
        });
    }
    
    public CompletableFuture<Boolean> setupToken(String token) {
        var saveConfigTask = CompletableFuture
                                .supplyAsync(() -> getBridge().getConfigurationService().set(configurationPathToken, token))
                                .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_CONFIG)));

        var stoppingTask = saveConfigTask
                            .thenCompose(isSuccess -> botState != BotState.Stopped ? stop() : CompletableFuture.completedFuture(true))
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_STOP_BOT)));

        var startingTask = stoppingTask
                            .thenCompose(isSuccess -> start())
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_START_BOT)));

        return startingTask;
    }

    public BotState getBotState() {
        return botState;
    }

    private String getTgToken() {
        return getBridge().getConfigurationService().get(configurationPathToken, "");
    }

    public void setupMaxRetries(int retries) {
        getBridge().getConfigurationService().set(configurationPathRetryMax, Integer.toString(retries));
    }

    private int getMaxRetry() {
        try {
            return Integer.parseInt(getBridge().getConfigurationService().get(configurationPathRetryMax, ""));
        }
        catch (NumberFormatException err) {
            return defaultRetryMax;
        }
    }

    public void setupRetryDelay(Duration delay) {
        getBridge().getConfigurationService().set(configurationPathRetryDelay, Long.toString(delay.toSeconds()));
    }

    private int getRetryDelay() {
        try {
            return Integer.parseInt(getBridge().getConfigurationService().get(configurationPathRetryDelay, ""));
        }
        catch (NumberFormatException err) {
            return defaultRetryDelay;
        }
    }

    @Override
    public void setAuthBridge(ISocialBridge bridge) {
        this.bridge = bridge;
    }

    public ISocialBridge getBridge() {
        return bridge;
    }

    @Override
    public String getPlatformName() {
        return "Telegram";
    }

    @Override
    public void sendMessage(SocialUser socialUser, String message, HashMap<String, String> placeholders) {
        var builder = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.newline())
                        .build());

        for (var placeholderKey : placeholders.keySet()) {
            builder.editTags(x -> x.resolver(Placeholder.component(placeholderKey, Component.text(placeholders.get(placeholderKey)))));
        }
        var builtMessage = builder.build().deserialize(message);

        var tgUser = (TelegramUser) socialUser;
        var chatId = tgUser.getLastMessage().getChat().getId();
        var replyToId = tgUser.getLastMessage().getMessageId();
        var msg = new SendMessage(chatId.toString(), message);
        msg.setReplyToMessageId(replyToId);
        msg.setParseMode(ParseMode.HTML);

        withRetries(() -> {
            if (botState == BotState.Started) {
                try {
                    telegramClient.execute(msg);
                    this.getBridge().getLogger().info("tgMessage to \"" + socialUser.getName() + "\" - " + builtMessage);
                }
                catch (TelegramApiException err) {
                    err.printStackTrace();
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    public Version getCompabilityVersion() {
        return socialBridgeCompabilityVersion;
    }

    private CompletableFuture<Boolean> withRetries(Callable<Boolean> callable) {
        return CompletableFuture.supplyAsync(() ->  {
            var retryCounter = 0;
            var delay = getRetryDelay();
            var maxRetries = getMaxRetry();

            while (retryCounter < maxRetries) {
                try {
                    if (callable.call()) {
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    retryCounter++;
                    try {
                        Thread.sleep(Duration.ofSeconds((int) Math.pow(delay, retryCounter)));
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        return false;
                    }
                }
            }
            return false;
        });
    }
}
