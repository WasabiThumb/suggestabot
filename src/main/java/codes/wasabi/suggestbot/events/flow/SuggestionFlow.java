package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.data.Suggestion;
import codes.wasabi.suggestbot.util.Codepoints;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;

public class SuggestionFlow extends Flow {

    private static final String[] set = new String[] {
            "https://wasabicodes.xyz/cdn/1968114c9246d5cf94432108f5f9dc32/blue.png",
            "https://wasabicodes.xyz/cdn/6d5949dbb92f3898e82346984a157506/green.png",
            "https://wasabicodes.xyz/cdn/5b7acc14d51dd474cde8c3de596e9f91/grey.png",
            "https://wasabicodes.xyz/cdn/1cc5f09db8eb6a6dd34c5d0998b88423/red.png",
            "https://wasabicodes.xyz/cdn/417f3d08986830e0b08c96fc71fcc0df/yellow.png"
    };
    private static final Random random = new Random();
    private static @NotNull String getRandomDefaultProfileImage() {
        return set[random.nextInt(set.length)];
    }

    private static @NotNull String[] resolveOwner(long id) {
        User user = null;
        try {
            user = Main.jda.retrieveUserById(id).complete();
        } catch (Exception ignored) { }
        if (user == null) return new String[]{ "Unknown User (ID: " + id + ")", getRandomDefaultProfileImage() };
        return new String[]{ "@" + user.getName() + "#" + user.getDiscriminator(), user.getEffectiveAvatarUrl() };
    }

    private static @NotNull MessageEmbed mainEmbed(@NotNull MessageChannel channel, @NotNull Suggestion suggestion, @NotNull User owner) {
        boolean upvoted = false;
        boolean downvoted = false;
        int upLen = 0;
        int downLen = 0;
        long ownerId = owner.getIdLong();
        for (long l : suggestion.getUpVotes()) {
            if (l == ownerId) upvoted = true;
            upLen++;
        }
        for (long l : suggestion.getDownVotes()) {
            if (l == ownerId) downvoted = true;
            downLen++;
        }
        String[] ownerDetails = resolveOwner(suggestion.getOwner());
        String conts = suggestion.getContent();
        conts = conts.substring(0, Math.min(conts.length(), 1800));
        EmbedBuilder eb = (new EmbedBuilder())
                .setColor(Color.ORANGE)
                .setTitle("Suggestion")
                .setDescription("By " + ownerDetails[0])
                .setThumbnail(ownerDetails[1])
                .addField("Content", conts, false)
                .addField("\uD83D\uDFE2 Upvote", (upvoted ? "**" : "") + upLen + (upvoted ? "**" : ""), true)
                .addField("\uD83D\uDD34 Downvote", (downvoted ? "**" : "") + downLen + (downvoted ? "**" : ""), true)
                .addField("\uD83D\uDD04 Refresh", "", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl());
        if (checkDeletePermission(channel, suggestion, owner)) {
            eb.addField("\uD83D\uDDD1 Delete", "", true);
        }
        return eb.build();
    }

    private static boolean checkDeletePermission(@NotNull Guild guild, @NotNull Suggestion suggestion, @NotNull User owner) {
        if (suggestion.getOwner() == owner.getIdLong()) return true;
        Member member = guild.getMember(owner);
        if (member == null) return false;
        return member.hasPermission(Permission.MESSAGE_MANAGE);
    }

    private static boolean checkDeletePermission(@NotNull MessageChannel channel, @NotNull Suggestion suggestion, @NotNull User owner) {
        if (suggestion.getOwner() == owner.getIdLong()) return true;
        if (channel instanceof GuildMessageChannel gmc) {
            Guild guild = gmc.getGuild();
            return checkDeletePermission(guild, suggestion, owner);
        }
        return false;
    }

    private static void mainReactions(@NotNull Message message, @NotNull Suggestion suggestion, @NotNull User owner) {
        message.addReaction(Codepoints.GREEN).complete();
        message.addReaction(Codepoints.RED).complete();
        message.addReaction(Codepoints.RELOAD).complete();
        boolean canDelete;
        if (message.isFromGuild()) {
            canDelete = checkDeletePermission(message.getGuild(), suggestion, owner);
        } else {
            canDelete = (suggestion.getOwner() == owner.getIdLong());
        }
        if (canDelete) message.addReaction(Codepoints.TRASH).complete();
    }

    private static @NotNull Message mainMessage(@NotNull MessageChannel channel, @NotNull Suggestion suggestion, @NotNull User owner) {
        Message message = channel.sendMessageEmbeds(mainEmbed(channel, suggestion, owner)).complete();
        Executors.newSingleThreadExecutor().submit(() -> mainReactions(message, suggestion, owner));
        return message;
    }

    private final MessageChannel channel;
    private final Suggestion suggestion;
    public SuggestionFlow(@NotNull MessageChannel channel, @NotNull Suggestion suggestion, @NotNull User owner) {
        super(mainMessage(channel, suggestion, owner), owner);
        this.channel = channel;
        this.suggestion = suggestion;
    }

    private void genericUpdate() {
        Message message = getMessage();
        User owner = getOwner();
        message.editMessageEmbeds(mainEmbed(channel, suggestion, owner)).complete();
        message.clearReactions().complete();
        mainReactions(message, suggestion, owner);
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
        MessageReaction.ReactionEmote emote = reaction.getReactionEmote();
        if (emote.isEmoji()) {
            String codepoints = emote.getAsCodepoints().toUpperCase(Locale.ROOT);
            User owner = getOwner();
            long ownerId = owner.getIdLong();
            switch (codepoints) {
                case Codepoints.GREEN -> runInThread(() -> {
                    boolean hasUpvote = false;
                    for (long u : suggestion.getUpVotes()) {
                        if (u == ownerId) {
                            hasUpvote = true;
                            break;
                        }
                    }
                    if (hasUpvote) {
                        suggestion.removeUpvote(ownerId);
                    } else {
                        suggestion.addUpvote(ownerId);
                    }
                    genericUpdate();
                });
                case Codepoints.RED -> runInThread(() -> {
                    boolean hasDownvote = false;
                    for (long u : suggestion.getDownVotes()) {
                        if (u == ownerId) {
                            hasDownvote = true;
                            break;
                        }
                    }
                    if (hasDownvote) {
                        suggestion.removeDownvote(ownerId);
                    } else {
                        suggestion.addDownvote(ownerId);
                    }
                    genericUpdate();
                });
                case Codepoints.RELOAD -> runInThread(this::genericUpdate);
                case Codepoints.TRASH -> {
                    if (checkDeletePermission(channel, suggestion, owner)) {
                        Main.db.getSuggestions().remove(suggestion);
                        close();
                    }
                }
            }
        }
    }

}
