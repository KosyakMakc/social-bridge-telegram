package io.github.kosyakmakc.socialBridgeTelegram.Utils;

import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

public class TelegramMessageKey {
    public static final MessageKey SET_TOKEN_SUCCESS = new MessageKey("set_token_success");
    public static final MessageKey SET_TOKEN_FAILED_CONFIG = new MessageKey("set_token_failed_config");
    public static final MessageKey SET_TOKEN_FAILED_STOP_BOT = new MessageKey("set_token_failed_stop_bot");
    public static final MessageKey SET_TOKEN_FAILED_START_BOT = new MessageKey("set_token_failed_start_bot");

    public static final MessageKey BOT_STATUS_CONNECTING = new MessageKey("bot_status_connecting");
    public static final MessageKey BOT_STATUS_CONNECTED = new MessageKey("bot_status_connected");
    public static final MessageKey BOT_STATUS_STOPPING = new MessageKey("bot_status_stopping");
    public static final MessageKey BOT_STATUS_STOPPED = new MessageKey("bot_status_stopped");

    // public static final MessageKey BOT_STARTED = new MessageKey("bot_started");
    // public static final MessageKey BOT_STOPPED = new MessageKey("bot_stopped");
}
