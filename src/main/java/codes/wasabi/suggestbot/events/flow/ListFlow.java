package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.util.Codepoints;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public abstract class ListFlow extends Flow {

    private int page = 0;

    private @NotNull MessageEmbed getEmbed(int from, int to) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.ORANGE);
        eb.setTitle(title);
        eb.setDescription("Page " + (page + 1));
        List<MessageEmbed.Field> list = getList();
        int len = list.size();
        for (int i=0; i < len; i++) {
            if (i < from) continue;
            if (i > to) break;
            eb.addField(list.get(i));
        }
        User owner = getOwner();
        eb.setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl());
        return eb.build();
    }

    private final String title;

    public ListFlow(@NotNull MessageChannel channel, @NotNull String title, @NotNull User owner) {
        super(channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Loading...").build()).complete(), owner);
        this.title = title;
        markStale();
        refresh();
    }

    private void refresh() {
        int len = getLength();
        int maxPage = Math.max(0, (int) Math.floor((len - 1) / 15d));
        page = Math.min(page, maxPage);
        Message message = getMessage();
        int startIndex = page * 15;
        message.editMessageEmbeds(getEmbed(startIndex, startIndex + 14)).queue();
        //
        Executors.newSingleThreadExecutor().submit(() -> {
            message.clearReactions().complete();
            if (page > 0) {
                message.addReaction("⬅").complete();
            }
            if (page < maxPage) {
                message.addReaction("➡").complete();
            }
            message.addReaction("\uD83D\uDD04").complete();
        });
    }

    protected abstract List<MessageEmbed.Field> getList();
    protected abstract void markStale();

    public final int getLength() {
        return getList().size();
    }

    @Override
    protected final void onReactionAdd(@NotNull MessageReaction reaction) {
        react(reaction);
    }

    @Override
    protected final void onReactionRemove(@NotNull MessageReaction reaction) {
        react(reaction);
    }

    private void react(@NotNull MessageReaction reaction) {
        MessageReaction.ReactionEmote emote = reaction.getReactionEmote();
        if (emote.isEmoji()) {
            String codepoint = emote.getAsCodepoints().toUpperCase(Locale.ROOT);
            if (codepoint.equals(Codepoints.LEFT)) {
                page--;
            } else if (codepoint.equals(Codepoints.RIGHT)) {
                page++;
            } else if (!codepoint.equals(Codepoints.RELOAD)) {
                return;
            } else {
                markStale();
            }
            refresh();
        }
    }

}
