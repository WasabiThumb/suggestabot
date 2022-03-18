package codes.wasabi.suggestbot.events;


import codes.wasabi.suggestbot.events.command.Command;
import codes.wasabi.suggestbot.events.flow.Flow;
import codes.wasabi.suggestbot.logging.Logger;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Events extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Logger.INFO.printLine("Bot is ready");
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        Logger.INFO.printLine("Bot reconnected successfully");
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        Logger.WARN.printLine("Bot lost connection!");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isWebhookMessage()) return;
        if (event.getAuthor().isBot()) return;
        if (Flow.Manager.acceptChatEvent(event)) return;
        String content = "";
        try {
            content = event.getMessage().getContentStripped();
        } catch (NullPointerException | UnsupportedOperationException ignored) { }
        if (content.startsWith(">]")) {
            String[] parts = content.substring(2).split("\\s+");
            if (parts.length > 0) {
                String cmd = parts[0];
                String[] args;
                if (parts.length > 1) {
                    args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                } else {
                    args = new String[0];
                }
                Command.execute(cmd, event.getChannel(), event.getAuthor(), args);
            }
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Flow.Manager.acceptReactionEvent(event);
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        Flow.Manager.acceptReactionEvent(event);
    }
}
