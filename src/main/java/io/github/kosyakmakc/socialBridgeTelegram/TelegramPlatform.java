package io.github.kosyakmakc.socialBridgeTelegram;

import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.time.Duration;
import java.util.HashMap;

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
    private static final int defaultRetryDelay = 10;

    private final Version socialBridgeCompabilityVersion = new Version("0.2.0");
    private final LongPollingHandler TgUpdatesHandler = new LongPollingHandler(this);
    private final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
    
    private TelegramClient telegramClient;
    private BotState botState = BotState.Stopped;

    private ISocialBridge bridge;

    @Override
    public void Start() {
        if (botState == BotState.Stopped) {
            return;
        }

        botState = BotState.Starting;

        var retryCounter = 0;
        var delay = getRetryDelay();
        var maxRetries = getMaxRetry();
        while (retryCounter < maxRetries) {
            var token = getTgToken();
            try {
                botsApplication.registerBot(token, TgUpdatesHandler);
                telegramClient = new OkHttpTelegramClient(token);
                botState = BotState.Started;
                return;
            } catch (TelegramApiException e) {
                e.printStackTrace();
                retryCounter++;
                try {
                    Thread.sleep(Duration.ofSeconds((int) Math.pow(delay, retryCounter)));
                } catch (InterruptedException e1) {
                }
            }
        }

        botState = BotState.Stopped;
    }

    public void stop() {
        if (botState == BotState.Started) {
            return;
        }
        botState = BotState.Stopping;
        
        var retryCounter = 0;
        var delay = getRetryDelay();
        var maxRetries = getMaxRetry();
        while (retryCounter < maxRetries) {
            var token = getTgToken();
            try {
                botsApplication.unregisterBot(token);
                telegramClient = null;
                botState = BotState.Stopped;
                return;
            } catch (TelegramApiException e) {
                e.printStackTrace();
                retryCounter++;
                try {
                    Thread.sleep(Duration.ofSeconds((int) Math.pow(delay, retryCounter)));
                } catch (InterruptedException e1) {
                }
            }
        }
        botState = BotState.Starting;
    }

    public void setupToken(String token) {
        getBridge().getConfigurationService().set(configurationPathToken, token);
        stop();
        Start();
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
                        .resolver(StandardTags.defaults())
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

        var retryCounter = 0;
        var delay = getRetryDelay();
        var maxRetries = getMaxRetry();
        while (retryCounter < maxRetries) {
            if (botState != BotState.Started) {
                return;
            }

            try {
                telegramClient.execute(msg);
                return;
            } catch (TelegramApiException e) {
                e.printStackTrace();
                retryCounter++;
                try {
                    Thread.sleep(Duration.ofSeconds((int) Math.pow(delay, retryCounter)));
                } catch (InterruptedException e1) {
                }
            }
        }
        this.getBridge().getLogger().info("tgMessage to \"" + socialUser.getName() + "\" - " + builtMessage);
    }

    @Override
    public Version getCompabilityVersion() {
        return socialBridgeCompabilityVersion;
    }
}
