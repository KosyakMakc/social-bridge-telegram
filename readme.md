# SocialBridge-telegram
## It is social platform connector to [telegram](https://telegram.org/) for [SocialBridge](https://github.com/KosyakMakc/social-bridge) minecraft plugin

### this connector provide commands for authorization processes

### Commands for minecraft:

| Command literal    | Permission node                 | Description                                                                   |
|--------------------|---------------------------------|-------------------------------------------------------------------------------|
| /telegram setToken | SocialBridge.telegram.setToken  | Save new token to SocialBridge configuration and reconnect bot with new token |
| /telegram status   | SocialBridge.telegram.status    | Provide information about current connection bot to telegram                  |

## API for developers

### You can connect API of this module for your purposes
```
repositories {
    maven {
        name = "gitea"
        url = "https://git.kosyakmakc.ru/api/packages/kosyakmakc/maven"
    }
}
dependencies {
    compileOnly "io.github.kosyakmakc:SocialBridge-telegram:0.5.2"
}
```

### via `ISocialBridge.getSocialPlatform(TelegramPlatform.class)` you can access this connector and use telegram-specific API