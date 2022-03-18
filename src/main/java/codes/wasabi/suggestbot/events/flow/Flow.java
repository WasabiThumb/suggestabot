package codes.wasabi.suggestbot.events.flow;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class Flow {

    public static class Manager {
        
        private static final Map<Long, Flow> registry = new HashMap<>();
        private static final Map<Integer, Flow> awaitingMessage = new HashMap<>();
        private static int keyHead = Integer.MIN_VALUE;
        private static Thread timeoutThread = null;
        
        public static boolean startTimeoutThread() {
            if (timeoutThread != null) {
                if (timeoutThread.isAlive()) return false;
            }
            timeoutThread = new Thread(() -> {
                boolean repeat = true;
                while (true) {
                    long curTime = System.currentTimeMillis();
                    long min = curTime - 60000L;
                    List<Flow> toClose = new ArrayList<>();
                    boolean anyClosed = false;
                    for (Flow f : registry.values()) {
                        if (f.lastUpdate <= min) {
                            if (f.keepAlive) continue;
                            toClose.add(f);
                            anyClosed = true;
                        }
                    }
                    for (Flow f : toClose) {
                        f.timeoutSwitch = true;
                        f.close();
                    }
                    if (anyClosed) System.gc();
                    try {
                        TimeUnit.SECONDS.sleep(30L);
                    } catch (InterruptedException e) {
                        repeat = false;
                    }
                }
            });
            timeoutThread.setName("Flow Timeout Thread");
            timeoutThread.start();
            return true;
        }

        public static boolean stopTimeoutThread() {
            if (timeoutThread == null) return false;
            if (!timeoutThread.isAlive()) return false;
            timeoutThread.interrupt();
            timeoutThread = null;
            return true;
        }

        public static void acceptReactionEvent(@NotNull MessageReactionAddEvent event) {
            long l = event.getMessageIdLong();
            Flow flow = registry.get(l);
            if (flow != null) {
                if (flow.lock) return;
                long user = event.getUserIdLong();
                if (flow.owner.getIdLong() == user) {
                    flow.lastUpdate = System.currentTimeMillis();
                    flow.onReactionAdd(event.getReaction());
                }
            }
        }

        public static void acceptReactionEvent(@NotNull MessageReactionRemoveEvent event) {
            long l = event.getMessageIdLong();
            Flow flow = registry.get(l);
            if (flow != null) {
                if (flow.lock) return;
                long user = event.getUserIdLong();
                if (flow.owner.getIdLong() == user) {
                    flow.lastUpdate = System.currentTimeMillis();
                    flow.onReactionRemove(event.getReaction());
                }
            }
        }

        public static boolean acceptChatEvent(@NotNull MessageReceivedEvent event) {
            long own = event.getAuthor().getIdLong();
            long chan = event.getChannel().getIdLong();
            if (awaitingMessage.size() > 0) {
                for (Flow f : awaitingMessage.values()) {
                    if (f.owner.getIdLong() == own) {
                        if (f.message.getChannel().getIdLong() == chan) {
                            f.msgFuture.complete(event.getMessage());
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
    }

    private long lastUpdate;
    private Message message;
    private long messageId;
    private final User owner;
    
    public Flow(@NotNull Message message, @NotNull User owner) {
        lastUpdate = System.currentTimeMillis();
        this.owner = owner;
        this.message = message;
        messageId = message.getIdLong();
        Manager.registry.put(messageId, this);
    }

    private int msgFutureKey = 0;
    private CompletableFuture<Message> msgFuture;

    private boolean keepAlive = false;

    protected final boolean getKeepAlive() {
        return keepAlive;
    }

    protected final void setKeepAlive(boolean keep) {
        keepAlive = keep;
    }

    protected final void keepAlive(@NotNull Runnable runnable) {
        boolean oldValue = getKeepAlive();
        setKeepAlive(true);
        try {
            runnable.run();
        } catch (Exception ignored) { } finally {
            lastUpdate = System.currentTimeMillis();
            setKeepAlive(oldValue);
        }
    }

    protected final @NotNull CompletableFuture<Message> awaitMessage() {
        int key = Manager.keyHead++;
        msgFutureKey = key;
        Manager.awaitingMessage.put(key, this);
        CompletableFuture<Message> ret = new CompletableFuture<>();
        msgFuture = ret;
        ret.whenComplete((Message s, Throwable t) -> {
            Manager.awaitingMessage.remove(key);
        });
        return ret;
    }

    protected final void clearReactions() {
        try {
            message.clearReactions().queue();
        } catch (Exception ignored) { }
    }

    protected final void fillReactions(@NotNull Emote... emotes) {
        for (Emote emote : emotes) {
            message.addReaction(emote).queue();
        }
    }

    protected final void fillReactions(@NotNull String... emotes) {
        for (String emote : emotes) {
            message.addReaction(emote).queue();
        }
    }

    public final @NotNull User getOwner() {
        return owner;
    }

    public final @NotNull Message getMessage() {
        return message;
    }

    protected final void switchMessage(@NotNull Message newMessage) {
        final long oldMessageId = messageId;
        try {
            message.delete().complete();
        } catch (Exception ignored) { }
        message = newMessage;
        messageId = newMessage.getIdLong();
        Manager.registry.remove(oldMessageId);
        Manager.registry.put(messageId, this);
        lastUpdate = System.currentTimeMillis();
    }

    private boolean timeoutSwitch = false;
    public final void close() {
        try {
            if (timeoutSwitch) {
                message.editMessage("*Dialog timed out*").queue();
                message.editMessageEmbeds().queue();
                message.clearReactions().queue();
            } else {
                message.delete().queue();
            }
        } catch (Exception ignored) { }
        Manager.registry.remove(messageId);
        Manager.awaitingMessage.remove(msgFutureKey);
    }

    private static int threadNum = 0;
    private boolean lock = false;
    protected final void runInThread(@NotNull Runnable runnable) {
        Thread t = new Thread(() -> {
            lock = true;
            try {
                runnable.run();
            } catch (Exception ignored) { }
            lock = false;
        });
        int num = threadNum++;
        t.setName("Flow Slave Thread #" + num);
        t.start();
    }

    protected abstract void onReactionAdd(@NotNull MessageReaction reaction);

    protected abstract void onReactionRemove(@NotNull MessageReaction reaction);

}
