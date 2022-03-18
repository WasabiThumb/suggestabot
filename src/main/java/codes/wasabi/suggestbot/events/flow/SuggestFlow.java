package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.data.Category;
import codes.wasabi.suggestbot.data.Suggestion;
import codes.wasabi.suggestbot.util.Codepoints;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class SuggestFlow extends Flow {

    private static final String[] assoc = new String[]{ "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "\uD83D\uDD1F" };
    private static final String[] cps = new String[]{ Codepoints.ONE, Codepoints.TWO, Codepoints.THREE, Codepoints.FOUR, Codepoints.FIVE, Codepoints.SIX, Codepoints.SEVEN, Codepoints.EIGHT, Codepoints.NINE, Codepoints.TEN };
    private static Map<Integer, Category> catMap;

    private static @NotNull Message createMainMessage(@NotNull MessageChannel channel, @NotNull User owner) {
        EmbedBuilder eb = (new EmbedBuilder())
                .setTitle("New Suggestion")
                .setColor(0xff00ff) // purple
                .setDescription("Select a category")
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl());
        int i = 0;
        Map<Integer, Category> map = new HashMap<>();
        for (Category c : Main.db.getCategories()) {
            eb.addField(assoc[i], c.getName() + (c.isLocked() ? " \uD83D\uDD12" : ""), false);
            map.put(i, c);
            i++;
            if (i > 9) break;
        }
        catMap = map;
        Message ret = channel.sendMessageEmbeds(eb.build()).complete();
        int finalI = i;
        Executors.newSingleThreadExecutor().submit(() -> {
            for (int z = 0; z < finalI; z++) {
                ret.addReaction(assoc[z]).complete();
            }
        });
        return ret;
    }

    private final MessageChannel channel;
    private int page = 0;
    public SuggestFlow(@NotNull MessageChannel channel, @NotNull User owner) {
        super(createMainMessage(channel, owner), owner);
        this.channel = channel;
    }

    @Override
    protected void onReactionAdd(@NotNull MessageReaction reaction) {
        react(reaction);
    }

    @Override
    protected void onReactionRemove(@NotNull MessageReaction reaction) {
        react(reaction);
    }

    private void react(@NotNull MessageReaction reaction) {
        Message message = getMessage();
        MessageReaction.ReactionEmote emote = reaction.getReactionEmote();
        if (emote.isEmoji()) {
            String codepoints = emote.getAsCodepoints().toUpperCase(Locale.ROOT);
            if (page == 0) {
                int i = -1;
                for (int z = 0; z < cps.length; z++) {
                    if (codepoints.equals(cps[z])) {
                        i = z;
                        break;
                    }
                }
                if (i < 0) return;
                Category cat = catMap.get(i);
                if (cat == null) return;
                if (cat.isLocked()) {
                    User owner = getOwner();
                    boolean privileged = false;
                    if (channel instanceof GuildMessageChannel gmc) {
                        Guild guild = gmc.getGuild();
                        Member member = guild.getMember(owner);
                        if (member != null) {
                            privileged = member.hasPermission(Permission.MESSAGE_MANAGE);
                        }
                    }
                    if (!privileged) {
                        channel.sendMessageEmbeds(
                                (new EmbedBuilder())
                                        .setColor(Color.RED)
                                        .setTitle("Error")
                                        .setDescription("This category is locked!")
                                        .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                                        .build()
                        ).queue();
                        return;
                    }
                }
                boolean found = false;
                for (Category c : Main.db.getCategories()) {
                    if (c == cat) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    User owner = getOwner();
                    channel.sendMessageEmbeds(
                            (new EmbedBuilder())
                                    .setColor(Color.RED)
                                    .setTitle("Error")
                                    .setDescription("The category you are trying to use no longer exists. Please try another one.")
                                    .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                                    .build()
                    ).queue();
                    return;
                }
                runInThread(() -> {
                    User owner = getOwner();
                    message.editMessageEmbeds(
                            (new EmbedBuilder())
                                    .setColor(Color.GREEN)
                                    .setTitle("Type your suggestion below")
                                    .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                                    .build()
                    ).complete();
                    message.clearReactions().queue();
                    AtomicReference<Message> response = new AtomicReference<>(null);
                    keepAlive(() -> {
                        try {
                            response.set(awaitMessage().get(300L, TimeUnit.SECONDS));
                        } catch (InterruptedException | TimeoutException | ExecutionException | CancellationException ignored) {
                            close();
                        }
                    });
                    Message resp = response.get();
                    if (resp == null) return;
                    StringBuilder conts = new StringBuilder(resp.getContentRaw());
                    for (Message.Attachment att : resp.getAttachments()) {
                        String url = att.getProxyUrl();
                        conts.append("\n").append(url);
                    }
                    Suggestion newSuggestion = Main.db.addSuggestion(cat, owner.getIdLong(), conts.toString());
                    message.editMessageEmbeds(
                            (new EmbedBuilder())
                                    .setColor(Color.GREEN)
                                    .setTitle("Successfully added your suggestion!")
                                    .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                                    .build()
                    ).complete();
                    Executors.newSingleThreadExecutor().submit(() -> {
                        new SuggestionFlow(channel, newSuggestion, owner);
                    });
                    page = 1;
                });
            }
        }
    }

}
