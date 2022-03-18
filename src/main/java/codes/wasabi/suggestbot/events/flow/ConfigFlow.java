package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.data.Category;
import codes.wasabi.suggestbot.util.Codepoints;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConfigFlow extends Flow {

    private static @NotNull MessageEmbed createMainEmbed(@NotNull User owner) {
        return (new EmbedBuilder())
                .setColor(Color.CYAN)
                .setTitle("Config")
                .addField("\uD83C\uDF0E", "General", true)
                .addField("\uD83D\uDCAC", "Channel Blacklist", true)
                .addField("\uD83D\uDCDA", "Categories", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                .build();
    }

    private static @NotNull MessageEmbed createGeneralEmbed(@NotNull User owner) {
        return (new EmbedBuilder())
                .setColor(Color.YELLOW)
                .setTitle("General Settings")
                .addField("\uD83D\uDED1", "Suggestion Limit", true)
                .addField("\uD83D\uDC48", "Go Back", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                .build();
    }

    private static @NotNull MessageEmbed createChannelEmbed(@NotNull User owner) {
        return (new EmbedBuilder())
                .setColor(Color.YELLOW)
                .setTitle("Blacklisted Channels")
                .addField("\uD83D\uDCD1", "List Channels", true)
                .addField("\uD83D\uDFE2", "Add Channel", true)
                .addField("\uD83D\uDD34", "Remove Channel", true)
                .addField("\uD83D\uDC48", "Go Back", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                .build();
    }

    private static @NotNull MessageEmbed createCategoryEmbed(@NotNull User owner) {
        return (new EmbedBuilder())
                .setColor(Color.YELLOW)
                .setTitle("Categories")
                .addField("\uD83D\uDCD1", "List Categories", true)
                .addField("\uD83D\uDFE2", "Add Category", true)
                .addField("\uD83D\uDD34", "Remove Category", true)
                .addField("\uD83D\uDD12", "Lock Category", true)
                .addField("\uD83D\uDD13", "Unlock Category", true)
                .addField("\uD83D\uDC48", "Go Back", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                .build();
    }

    private static @NotNull Message createMainMessage(@NotNull MessageChannel channel, @NotNull User owner) {
        Message ret = channel.sendMessageEmbeds(createMainEmbed(owner)).complete();
        ret.addReaction(Codepoints.GLOBE).queue();
        ret.addReaction(Codepoints.SPEECH_BUBBLE).queue();
        ret.addReaction(Codepoints.BOOKS).queue();
        return ret;
    }

    private final MessageChannel channel;
    private int page = 0;

    public ConfigFlow(@NotNull MessageChannel channel, @NotNull User owner) {
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

    private void returnToMain() {
        Message message = getMessage();
        message.editMessageEmbeds(createMainEmbed(getOwner())).complete();
        page = 0;
        message.clearReactions().complete();
        message.addReaction(Codepoints.GLOBE).queue();
        message.addReaction(Codepoints.SPEECH_BUBBLE).queue();
        message.addReaction(Codepoints.BOOKS).queue();
    }

    private void react(@NotNull MessageReaction reaction) {
        Message message = getMessage();
        MessageReaction.ReactionEmote emote = reaction.getReactionEmote();
        if (emote.isEmoji()) {
            String codepoint = emote.getAsCodepoints().toUpperCase(Locale.ROOT);
            if (page == 0) {
                switch (codepoint) {
                    case Codepoints.GLOBE -> runInThread(() -> {
                        message.editMessageEmbeds(createGeneralEmbed(getOwner())).complete();
                        page = 1;
                        message.clearReactions().complete();
                        message.addReaction(Codepoints.STOP).complete();
                        message.addReaction(Codepoints.BACK).complete();
                    });
                    case Codepoints.SPEECH_BUBBLE -> runInThread(() -> {
                        message.editMessageEmbeds(createChannelEmbed(getOwner())).complete();
                        page = 2;
                        message.clearReactions().complete();
                        message.addReaction(Codepoints.BOOKMARK).complete();
                        message.addReaction(Codepoints.GREEN).complete();
                        message.addReaction(Codepoints.RED).complete();
                        message.addReaction(Codepoints.BACK).complete();
                    });
                    case Codepoints.BOOKS -> runInThread(() -> {
                        message.editMessageEmbeds(createCategoryEmbed(getOwner())).complete();
                        page = 3;
                        message.clearReactions().complete();
                        message.addReaction(Codepoints.BOOKMARK).complete();
                        message.addReaction(Codepoints.GREEN).complete();
                        message.addReaction(Codepoints.RED).complete();
                        message.addReaction(Codepoints.LOCK).complete();
                        message.addReaction(Codepoints.UNLOCK).complete();
                        message.addReaction(Codepoints.BACK).complete();
                    });
                }
            } else if (page == 1) {
                switch (codepoint) {
                    case Codepoints.STOP -> runInThread(() -> {
                        message.clearReactions().complete();
                        message.editMessageEmbeds(
                                (new EmbedBuilder())
                                        .setColor(Color.GREEN)
                                        .setTitle("Please enter the new limit, or anything else for no limit.")
                                        .build()
                        ).complete();
                        String msg = "";
                        int lim = -1;
                        try {
                            msg = awaitMessage().get(60L, TimeUnit.SECONDS).getContentStripped();
                        } catch (InterruptedException | ExecutionException ignored) {
                        } catch (TimeoutException e) {
                            channel.sendMessage("*Timed out*").queue();
                            close();
                            return;
                        }
                        try {
                            lim = Integer.parseInt(msg);
                        } catch (NumberFormatException ignored) {}
                        Main.db.getSettings().setMaxSuggestionsPerUser(lim);
                        channel.sendMessage("Successfully set max suggestions to " + lim).queue();
                        message.editMessageEmbeds(createGeneralEmbed(getOwner())).complete();
                        message.addReaction(Codepoints.STOP).complete();
                        message.addReaction(Codepoints.BACK).complete();
                    });
                    case Codepoints.BACK -> runInThread(this::returnToMain);
                }
            } else if (page == 2) {
                switch (codepoint) {
                    case Codepoints.BOOKMARK -> new BlacklistFlow(channel, getOwner());
                    case Codepoints.GREEN -> runInThread(() -> {
                        GuildMessageChannel gmc = queryChannel();
                        if (gmc != null) {
                            Main.db.getSettings().addDisallowedChannel(gmc.getIdLong());
                            channel.sendMessageEmbeds(
                                    (new EmbedBuilder())
                                            .setColor(Color.GREEN)
                                            .setTitle("Channel Added to Blacklist")
                                            .setDescription(gmc.getAsMention())
                                            .build()
                            ).queue();
                        }
                    });
                    case Codepoints.RED -> runInThread(() -> {
                        GuildMessageChannel gmc = queryChannel();
                        if (gmc != null) {
                            Main.db.getSettings().removeDisallowedChannel(gmc.getIdLong());
                            channel.sendMessageEmbeds(
                                    (new EmbedBuilder())
                                            .setColor(Color.RED)
                                            .setTitle("Channel Removed from Blacklist")
                                            .setDescription(gmc.getAsMention())
                                            .build()
                            ).queue();
                        }
                    });
                    case Codepoints.BACK -> runInThread(this::returnToMain);
                }
            } else if (page == 3) {
                boolean adding = false;
                boolean locking = false;
                boolean modifying = false;
                switch (codepoint) {
                    case Codepoints.BOOKMARK:
                        new CategoryFlow(channel, getOwner());
                        break;
                    case Codepoints.LOCK:
                        locking = true;
                    case Codepoints.UNLOCK:
                        modifying = true;
                    case Codepoints.GREEN:
                        adding = true;
                    case Codepoints.RED:
                        final boolean _adding = adding;
                        final boolean _locking = locking;
                        final boolean _modifying = modifying;
                        runInThread(() -> {
                            message.clearReactions().complete();
                            message.editMessageEmbeds(
                                    (new EmbedBuilder())
                                            .setColor(Color.GREEN)
                                            .setTitle("Enter a category name")
                                            .build()
                            ).complete();
                            String st;
                            try {
                                st = awaitMessage().get(60L, TimeUnit.SECONDS).getContentStripped();
                            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                                channel.sendMessage("*Timed out*").queue();
                                close();
                                return;
                            }
                            Category cat = Main.db.getCategory(st);
                            if (_modifying || (!_adding)) {
                                if (cat == null) {
                                    channel.sendMessageEmbeds(
                                            (new EmbedBuilder())
                                                    .setColor(Color.RED)
                                                    .setTitle("No category by that name!")
                                                    .build()
                                    ).queue();
                                } else {
                                    if (_modifying) {
                                        cat.setLocked(_locking);
                                        channel.sendMessageEmbeds(
                                                (new EmbedBuilder())
                                                        .setColor(Color.GREEN)
                                                        .setTitle((_locking ? "L" : "Unl") + "ocked category " + cat.getName())
                                                        .build()
                                        ).queue();
                                    } else {
                                        String n = cat.getName();
                                        Main.db.removeCategory(n);
                                        channel.sendMessageEmbeds(
                                                (new EmbedBuilder())
                                                        .setColor(Color.GREEN)
                                                        .setTitle("Removed category " + n)
                                                        .build()
                                        ).queue();
                                    }
                                }
                            } else {
                                if (cat != null) {
                                    channel.sendMessageEmbeds(
                                            (new EmbedBuilder())
                                                    .setColor(Color.RED)
                                                    .setTitle("A category with that name already exists!")
                                                    .build()
                                    ).queue();
                                } else {
                                    Main.db.createCategory(st);
                                    channel.sendMessageEmbeds(
                                            (new EmbedBuilder())
                                                    .setColor(Color.GREEN)
                                                    .setTitle("Created category " + st)
                                                    .build()
                                    ).queue();
                                }
                            }
                            message.editMessageEmbeds(createCategoryEmbed(getOwner())).complete();
                            page = 3;
                            message.addReaction(Codepoints.BOOKMARK).complete();
                            message.addReaction(Codepoints.GREEN).complete();
                            message.addReaction(Codepoints.RED).complete();
                            message.addReaction(Codepoints.LOCK).complete();
                            message.addReaction(Codepoints.UNLOCK).complete();
                            message.addReaction(Codepoints.BACK).complete();
                        });
                        break;
                    case Codepoints.BACK:
                        runInThread(this::returnToMain);
                        break;
                }
            }
        }
    }

    private @Nullable GuildMessageChannel queryChannel() {
        Message message = getMessage();
        message.editMessageEmbeds(
                (new EmbedBuilder())
                        .setColor(Color.GREEN)
                        .setTitle("Enter a channel")
                        .setDescription("You can use a mention, name or ID")
                        .build()
        ).complete();
        message.clearReactions().queue();
        String str;
        try {
            str = awaitMessage().get(60L, TimeUnit.SECONDS).getContentRaw();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return null;
        } catch (TimeoutException e) {
            channel.sendMessage("*Timed out*").queue();
            close();
            return null;
        }
        GuildMessageChannel ret = null;
        if (channel instanceof GuildMessageChannel gmc) {
            Guild guild = gmc.getGuild();
            for (GuildChannel gc : guild.getChannels()) {
                if (gc instanceof GuildMessageChannel gmc1) {
                    if (str.contains(gmc1.getId())) {
                        ret = gmc1;
                        break;
                    } else if (str.toLowerCase(Locale.ROOT).startsWith(gmc1.getName().toLowerCase(Locale.ROOT))) {
                        ret = gmc1;
                        break;
                    }
                }
            }
        }
        //
        message.editMessageEmbeds(createChannelEmbed(getOwner())).complete();
        page = 2;
        message.clearReactions().complete();
        message.addReaction(Codepoints.BOOKMARK).complete();
        message.addReaction(Codepoints.GREEN).complete();
        message.addReaction(Codepoints.RED).complete();
        message.addReaction(Codepoints.BACK).complete();
        //
        return ret;
    }


}
