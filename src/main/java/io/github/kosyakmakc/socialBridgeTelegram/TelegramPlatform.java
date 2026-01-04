package io.github.kosyakmakc.socialBridgeTelegram;

import io.github.kosyakmakc.socialBridge.ISocialBridge;
import io.github.kosyakmakc.socialBridge.ISocialModule;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.ISocialCommand;
import io.github.kosyakmakc.socialBridge.DatabasePlatform.LocalizationService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.j256.ormlite.table.TableUtils;

public class TelegramPlatform implements ISocialPlatform {
    private static final String PLATFORM_NAME = "telegram";
    private static final String configurationPath = "social-bridge-telegram";
    private static final String configurationPathToken = configurationPath + "_token";
    private static final String configurationPathRetryMax = configurationPath + "_retries-max";
    private static final int defaultRetryMax = 10;
    private static final String configurationPathRetryDelay = configurationPath + "_retries-delay";
    private static final int defaultRetryDelay = 2;

    private final CacheContainer<TelegramUser> userCaching = new CacheContainer<>(500);

    private final Version socialBridgeCompabilityVersion = new Version("0.5.0");
    private final TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
    
    private BotState botState = BotState.Stopped;
    private LongPollingHandler telegramHandler;
    private OkHttpTelegramClient telegramClient;
    private String usingToken;

    private LinkedList<ISocialModule> connectedModules = new LinkedList<>();

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
                        telegramHandler = new LongPollingHandler(this);
                        botsApplication.registerBot(token, telegramHandler);
                        usingToken = token;

                        var userBot = telegramClient.execute(new GetMe());
                        telegramHandler.setBotUsername(userBot.getUserName());

                        botState = BotState.Started;
                        logger.info("Telegram bot connected");
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                });
        })
        .thenComposeAsync(isSuccessStart -> {
            if (isSuccessStart) {
                return updateCommandSuggestions();
            }
            else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }

    public CompletableFuture<Boolean> stopBot() {
        if (botState != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        logger.info("Telegram bot stopping...");

        botState = BotState.Stopping;

        try {
            botsApplication.unregisterBot(usingToken);
            botState = BotState.Stopped;
            telegramClient = null;
            telegramHandler = null;
            logger.info("Telegram bot stopped");
        }
        catch (TelegramApiException err) {
            err.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
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
        return PLATFORM_NAME;
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

        var resolvedComponents = builder.build().deserialize(message);
        var htmlBuiltMessage = MiniMessage.miniMessage().serialize(resolvedComponents);

        var tgUser = (TelegramUser) socialUser;
        var chatId = tgUser.getLastMessage().getChat().getId();
        var replyToId = tgUser.getLastMessage().getMessageId();
        var msg = new SendMessage(chatId.toString(), htmlBuiltMessage.toString());
        msg.setReplyToMessageId(replyToId);
        msg.setParseMode(ParseMode.HTML);

        return withRetries(() -> {
            if (botState == BotState.Started) {
                try {
                    telegramClient.execute(msg);
                    logger.info("tgMessage to \"" + socialUser.getName() + "\" - " + htmlBuiltMessage);
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
    public CompletableFuture<Void> connectModule(ISocialModule module) {
        connectedModules.add(module);
        return updateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<Void> disconnectModule(ISocialModule module) {
        var isRemoved = connectedModules.remove(module);
        if (isRemoved) {
            return updateCommandSuggestions().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void
        }
        else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final Pattern TelegramValidCommandToken = Pattern.compile("^[a-z0-9_]{1,32}$");

    private CompletableFuture<Boolean> updateCommandSuggestions() {
        var languages = new HashSet<String>();
        for (var module : connectedModules) {
            for (var translationSource : module.getTranslations()) {
                languages.add(translationSource.getLanguage());
            }
        }

        @SuppressWarnings("unchecked")
        var commandInfos = connectedModules
            .stream()
            .mapMulti((x, consumer) -> {
                var matcher = TelegramValidCommandToken.matcher(x.getName());
                if (!matcher.find()) {
                    logger.warning("module name '" + x.getName() + "' not valid for telegram suggestion, skips all his command from suggestion. But commands is keep working");
                    return;
                }

                x.getSocialCommands().forEach(y -> consumer.accept(new ImmutablePair<>(x, y)));
            })
            .map(x -> (ImmutablePair<ISocialModule, ISocialCommand>) x)
            .map(pair -> {
                var finalName = pair.left.getName() + '_' + pair.right.getLiteral();
                var matcher = TelegramValidCommandToken.matcher(finalName);
                if (!matcher.find()) {
                    logger.warning("telegram command name '" + finalName + "' not valid for telegram suggestion, skips this command from suggestion. But command is keep working");
                    return null;
                }

                return pair;
            })
            .toList();

        return CompletableFuture
            .allOf(languages.stream().map(x -> updateCommandSuggestions(x, commandInfos)).toArray(CompletableFuture[]::new))
            .thenRun(() -> updateCommandSuggestions(null, commandInfos))
            .thenApply(Void -> true);
    }

    private CompletableFuture<Boolean> updateCommandSuggestions(String languageCode, List<ImmutablePair<ISocialModule, ISocialCommand>> commands) {
        if (getBotState() != BotState.Started) {
            return CompletableFuture.completedFuture(false);
        }

        @SuppressWarnings("unchecked")
        var tasks = (CompletableFuture<BotCommand>[]) commands
            .stream()
            .map(pair -> {
                var finalName = pair.left.getName() + '_' + pair.right.getLiteral();

                var localizationLanguage = languageCode != null
                                            ? languageCode
                                            : LocalizationService.defaultLocale;

                return bridge.getLocalizationService()
                    .getMessage(pair.left, localizationLanguage, pair.right.getDescription())
                    .thenApply(description -> new BotCommand(finalName, description));
            })
            .toArray(CompletableFuture[]::new);

        return CompletableFuture
            .allOf(tasks)
            .thenCompose(Void -> {
                var commandsInfo = Arrays
                    .stream(tasks)
                    .map(task -> {
                        try {
                            return task.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(commandInfo -> commandInfo != null)
                    .toList();

                if (commandsInfo.isEmpty()) {
                    return CompletableFuture.completedFuture(true);
                }
                else {
                    var commandQuery = new SetMyCommands(commandsInfo);
                    commandQuery.setLanguageCode(languageCode);
                    return this.withRetries(() -> telegramClient.execute(commandQuery));
                }
            });
    }

    @Override
    public CompletableFuture<Boolean> enable(ISocialBridge socialBridge) {
        this.bridge = socialBridge;
        logger = Logger.getLogger(this.bridge.getLogger().getName() + '.' + TelegramPlatform.class.getSimpleName());

        var initTask = this.bridge.queryDatabase(ctx -> {
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
        });

        // starting in background
        initTask.thenCompose(d -> startBot());

        return initTask;
    }

    @Override
    public CompletableFuture<Void> disable() {
        this.bridge = null;
        return stopBot().thenRun(() -> {}); // empty runnable for resolve return type from boolean to Void 
    }

    @Override
    public CompletableFuture<SocialUser> tryGetUser(Identifier id) {
        var cachedUser = userCaching.tryGet(x -> (long) x.getId().value() == (long) id.value());
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
            userCaching.checkAndAdd(user); // second search on cache, maybe item has been added while async operates
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
