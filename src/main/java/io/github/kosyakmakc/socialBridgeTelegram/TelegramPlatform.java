package io.github.kosyakmakc.socialBridgeTelegram;

import io.github.kosyakmakc.socialBridge.IBridgeModule;
import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.Identifier;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.SocialUser;
import io.github.kosyakmakc.socialBridge.Utils.Version;
import io.github.kosyakmakc.socialBridgeTelegram.DatabaseTables.TelegramUserTable;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.CacheContainer;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TelegramMessageKey;
import io.github.kosyakmakc.socialBridgeTelegram.Utils.TranslationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.j256.ormlite.table.TableUtils;

public class TelegramPlatform implements ISocialPlatform {
    private static final String configurationPath = "social-bridge-telegram";
    private static final String configurationPathToken = configurationPath + "_token";
    private static final String configurationPathRetryMax = configurationPath + "_retries-max";
    private static final int defaultRetryMax = 10;
    private static final String configurationPathRetryDelay = configurationPath + "_retries-delay";
    private static final int defaultRetryDelay = 2;

    private static final int userCacheSize = 500;
    private final CacheContainer<TelegramUser> userCaching = new CacheContainer<>();

    private final Version socialBridgeCompabilityVersion = new Version("0.3.0");
    private final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
    
    private BotState botState = BotState.Stopped;
    private LongPollingHandler telegramHandler;
    private OkHttpTelegramClient telegramClient;

    private LinkedList<IBridgeModule> connectedModules = new LinkedList<>();

    private ISocialBridge bridge;
    private Logger logger;

    public CompletableFuture<Boolean> startBot() {
        if (botState != BotState.Stopped) {
            return CompletableFuture.completedFuture(false);
        }

        logger.info("Telegram bot starting...");

        botState = BotState.Starting;
        return getTgToken()
            .thenCompose(token -> {
                if (token.isBlank()) {
                    logger.info("Token missed, connect to telegram canceled");
                    botState = BotState.Stopped;
                    return CompletableFuture.completedFuture(false);
                }

                return withRetries(() -> {
                    try {
                        telegramClient = new OkHttpTelegramClient(token);
                        // telegramClient.execute(new SetMyCommands(null))
                        telegramHandler = new LongPollingHandler(this);
                        botsApplication.registerBot(token, telegramHandler);

                        botState = BotState.Started;
                        logger.info("Telegram bot connected");
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                });
        });
    }

    public CompletableFuture<Boolean> stopBot() {
        if (botState != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        logger.info("Telegram bot stopping...");

        botState = BotState.Stopping;

        return getTgToken()
        .thenCompose(token -> {
            return withRetries(() -> {
                try {
                    botsApplication.unregisterBot(token);
                    botState = BotState.Stopped;
                    telegramClient = null;
                    telegramHandler = null;
                    logger.info("Telegram bot stopped");
                }
                catch (TelegramApiException err) {
                    err.printStackTrace();
                    return false;
                }
                return true;
            });
        });
    }
    
    public CompletableFuture<Boolean> setupToken(String token) {
        var tgModule = getTelegramModule();

        var saveConfigTask =  getBridge().getConfigurationService().set(tgModule, configurationPathToken, token)
                             .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_CONFIG)));

        var stoppingTask = saveConfigTask
                            .thenCompose(isSuccess -> botState != BotState.Stopped ? stopBot() : CompletableFuture.completedFuture(true))
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_STOP_BOT)));

        var startingTask = stoppingTask
                            .thenCompose(isSuccess -> startBot())
                            .thenCompose(isSuccess -> isSuccess ? CompletableFuture.completedFuture(isSuccess) : CompletableFuture.failedFuture(new TranslationException(TelegramMessageKey.SET_TOKEN_FAILED_START_BOT)));

        return startingTask;
    }

    public BotState getBotState() {
        return botState;
    }

    private CompletableFuture<String> getTgToken() {
        var tgModule = getTelegramModule();
        return getBridge().getConfigurationService().get(tgModule, configurationPathToken, "");
    }

    public CompletableFuture<Boolean> setupMaxRetries(int retries) {
        var tgModule = getTelegramModule();
        return getBridge().getConfigurationService().set(tgModule, configurationPathRetryMax, Integer.toString(retries));
    }

    private CompletableFuture<Integer> getMaxRetry() {
        var tgModule = getTelegramModule();
        return getBridge().getConfigurationService().get(tgModule, configurationPathRetryMax, "")
              .thenApply(rawNumber -> {
                  try {
                      return Integer.parseInt(rawNumber);
                  }
                  catch (NumberFormatException err) {
                      return defaultRetryMax;
                  }
              });
    }

    public CompletableFuture<Boolean> setupRetryDelay(Duration delay) {
        var tgModule = getTelegramModule();
        return getBridge().getConfigurationService().set(tgModule, configurationPathRetryDelay, Long.toString(delay.toSeconds()));
    }

    private CompletableFuture<Integer> getRetryDelay() {
        var tgModule = getTelegramModule();
        return getBridge().getConfigurationService().get(tgModule, configurationPathRetryDelay, "")
              .thenApply(rawNumber -> {
                  try {
                      return Integer.parseInt(rawNumber);
                  }
                  catch (NumberFormatException err) {
                      return defaultRetryDelay;
                  }
              });
    }

    public ISocialBridge getBridge() {
        return bridge;
    }

    @Override
    public String getPlatformName() {
        return "Telegram";
    }

    @Override
    public CompletableFuture<Boolean> sendMessage(SocialUser socialUser, String message, HashMap<String, String> placeholders) {
        var builder = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.newline())
                        .build());

        for (var placeholderKey : placeholders.keySet()) {
            builder.editTags(x -> x.resolver(Placeholder.component(placeholderKey, Component.text(placeholders.get(placeholderKey)))));
        }
        var builtMessage = builder.build().deserialize(message).toString();

        var tgUser = (TelegramUser) socialUser;
        var chatId = tgUser.getLastMessage().getChat().getId();
        var replyToId = tgUser.getLastMessage().getMessageId();
        var msg = new SendMessage(chatId.toString(), builtMessage);
        msg.setReplyToMessageId(replyToId);
        msg.setParseMode(ParseMode.HTML);

        return withRetries(() -> {
            if (botState == BotState.Started) {
                try {
                    telegramClient.execute(msg);
                    logger.info("tgMessage to \"" + socialUser.getName() + "\" - " + builtMessage);
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
            var delay = getRetryDelay().join();
            var maxRetries = getMaxRetry().join();

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

    @Override
    public CompletableFuture<Void> connectModule(IBridgeModule module) {
        connectedModules.add(module);
        return UpdateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<Void> disconnectModule(IBridgeModule module) {
        var isRemoved = connectedModules.remove(module);
        if (isRemoved) {
            return UpdateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Boolean> UpdateCommandSuggestions() {
        @SuppressWarnings("unchecked")
        var commandsInfo = connectedModules
            .stream()
            .mapMulti((x, consumer) -> {
                x.getSocialCommands().forEach(y -> consumer.accept(new ImmutablePair<>(x, y)));
            })
            .map(x -> (ImmutablePair<IBridgeModule, ISocialCommand>) x)
            .filter(x -> x != null)
            .map(pair -> new BotCommand('/' + pair.left.getName() + '-' + pair.right.getLiteral(), pair.right.getLiteral()))
            .toList();

        var commandQuery = new SetMyCommands(commandsInfo);
        return this.withRetries(() -> telegramClient.execute(commandQuery));
    }

    @Override
    public CompletableFuture<Boolean> enable(ISocialBridge socialBridge) {
        this.bridge = socialBridge;
        logger = Logger.getLogger(this.bridge.getLogger().getName() + '.' + TelegramPlatform.class.getSimpleName());

        return this.bridge.queryDatabase(ctx -> {
            try {
                TableUtils.createTableIfNotExists(ctx.getConnectionSource(), TelegramUserTable.class);
                var daoSession = ctx.registerTable(TelegramUserTable.class);
                
                if (daoSession == null) {
                    throw new RuntimeException("Failed to create required database table - " + TelegramUserTable.class.getSimpleName());
                }

                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                this.bridge = null;
                return false;
            }
        }).thenCompose(d -> startBot());
    }

    @Override
    public CompletableFuture<Void> disable() {
        this.bridge = null;
        return stopBot().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<SocialUser> tryGetUser(Identifier id) {
        var cachedUser = userCaching.TryGet(x -> (long) x.getId().value() == (long) id.value());
        if (cachedUser != null) {
            return CompletableFuture.completedFuture(cachedUser);
        }

        return this.bridge.queryDatabase(ctx -> {
            var dao = ctx.getDaoTable(TelegramUserTable.class);

            TelegramUserTable dbUser;
            try {
                dbUser = dao.queryForId((long) id.value());
            } catch (SQLException e) {
                e.printStackTrace();
                dbUser = null;
            }

            if (dbUser == null) {
                return null;
            }

            var user = new TelegramUser(this, dbUser);
            userCaching.CheckAndAdd(user); // second search on cache, maybe item has been added while async operates
            return user;
        });
    }

    private TelegramModule getTelegramModule() {
        if (bridge == null) {
            throw new RuntimeException("Telegram platform has been disconnected from SocialBridge");
        }

        var module = bridge.getModule(TelegramModule.class);

        if (module == null) {
            throw new RuntimeException("Required telegram module not connected to SocialBridge");
        }

        return module;
    }

    public Logger getLogger() {
        return logger;
    }
}
