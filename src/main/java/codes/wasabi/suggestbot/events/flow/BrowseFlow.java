package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.data.Suggestion;
import codes.wasabi.suggestbot.util.Codepoints;
import codes.wasabi.suggestbot.util.PeriodFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BrowseFlow extends Flow {

    private static final String[] NUM_UNI = new String[]{ "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "\uD83D\uDD1F" };
    private static final String[] NUM_CP = new String[]{ Codepoints.ONE, Codepoints.TWO, Codepoints.THREE, Codepoints.FOUR, Codepoints.FIVE, Codepoints.SIX, Codepoints.SEVEN, Codepoints.EIGHT, Codepoints.NINE, Codepoints.TEN };

    private static @NotNull String resolveOwner(long id) {
        User user = null;
        try {
            user = Main.jda.retrieveUserById(id).complete();
        } catch (Exception ignored) { }
        if (user == null) return "Unknown User (ID: " + id + ")";
        return "@" + user.getName() + "#" + user.getDiscriminator();
    }

    public static @NotNull MessageEmbed mainEmbed(@NotNull User owner) {
        return (new EmbedBuilder())
                .setColor(Color.CYAN)
                .setTitle("Browse Suggestions")
                .addField("\uD83D\uDD25", "Hot", true)
                .addField("⬆", "Top", true)
                .addField("✨", "New", true)
                .addField("\uD83D\uDE08", "Controversial", true)
                .addField("\uD83D\uDD8C", "My Suggestions", true)
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl())
                .build();
    }

    public static void mainReactions(@NotNull Message message) {
        message.addReaction(Codepoints.FIRE).complete();
        message.addReaction(Codepoints.UP).complete();
        message.addReaction(Codepoints.SPARKLES).complete();
        message.addReaction(Codepoints.IMP).complete();
        message.addReaction(Codepoints.BRUSH).complete();
    }

    public static @NotNull Message mainMessage(@NotNull MessageChannel channel, @NotNull User owner) {
        Message message = channel.sendMessageEmbeds(mainEmbed(owner)).complete();
        Executors.newSingleThreadExecutor().submit(() -> mainReactions(message));
        return message;
    }

    private int page = 0;
    private int subPage = 0;
    private ListType curListType;
    private List<Suggestion> curList;
    private final Map<Integer, Suggestion> assoc = new HashMap<>();
    private final MessageChannel channel;
    public BrowseFlow(@NotNull MessageChannel channel, @NotNull User owner) {
        super(mainMessage(channel, owner), owner);
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

    private enum ListType {
        HOT(1, "Hot", Comparator.comparingDouble(Suggestion::getHeat).reversed()),
        TOP(2, "Top", Comparator.comparingInt(Suggestion::getVoteCount).reversed()),
        NEW(3, "New", Comparator.comparingLong(Suggestion::getCreationTime).reversed()),
        CONTRA(4, "Controversial", Comparator.comparingDouble(Suggestion::getHeat)),
        MINE(5, "Your Suggestions", Comparator.comparingLong(Suggestion::getCreationTime).reversed());

        private final int index;
        private final String name;
        private final Comparator<Suggestion> sortFunc;
        ListType(int index, String name, Comparator<Suggestion> sortFunc) {
            this.index = index;
            this.name = name;
            this.sortFunc = sortFunc;
        }

        List<Suggestion> getList(@NotNull User owner) {
            long id = owner.getIdLong();
            return Main.db.getSuggestions().stream().filter((Suggestion sug) -> (index != 5 || sug.getOwner() == id)).sorted(sortFunc).collect(Collectors.toList());
        }
    }

    private void enterList(@NotNull ListType lt) {
        curListType = lt;
        curList = lt.getList(getOwner());
        subPage = 0;
        populateList();
        page = lt.index;
    }

    private void populateList() {
        int listLength = curList.size();
        int maxPage = Math.max((int) Math.floor((listLength - 1) / 10d), 0);
        subPage = Math.max(Math.min(maxPage, subPage), 0);
        int fromIndex = subPage * 10;
        int toIndex = Math.min(fromIndex + 10, listLength);
        User owner = getOwner();
        EmbedBuilder eb = (new EmbedBuilder())
                .setColor(Color.BLUE)
                .setTitle("Browse Suggestions")
                .setDescription(curListType.name + " • Page " + (subPage + 1))
                .setFooter("Requested by @" + owner.getName() + "#" + owner.getDiscriminator(), owner.getEffectiveAvatarUrl());
        int num = 0;
        assoc.clear();
        for (int z = fromIndex; z < toIndex; z++) {
            Suggestion sug = curList.get(z);
            assoc.put(num, sug);
            String content = sug.getContent();
            if (content.length() > 50) {
                content = content.substring(0, 50) + "…";
            }
            eb.addField(NUM_UNI[num] + " [" + sug.getCategory().getName() + "] " + content, PeriodFormatter.format(System.currentTimeMillis() - sug.getCreationTime()) + " by " + resolveOwner(sug.getOwner()), false);
            num++;
        }
        Message message = getMessage();
        message.editMessageEmbeds(eb.build()).complete();
        message.clearReactions().complete();
        if (subPage > 0) {
            message.addReaction(Codepoints.LEFT).complete();
        }
        if (subPage < maxPage) {
            message.addReaction(Codepoints.RIGHT).complete();
        }
        for (int z = 0; z < num; z++) {
            message.addReaction(NUM_CP[z]).complete();
        }
        message.addReaction(Codepoints.RELOAD).complete();
        message.addReaction(Codepoints.BACK).complete();
    }

    private void react(@NotNull MessageReaction reaction) {
        MessageReaction.ReactionEmote emote = reaction.getReactionEmote();
        if (emote.isEmoji()) {
            String codepoints = emote.getAsCodepoints().toUpperCase(Locale.ROOT);
            if (page == 0) {
                switch (codepoints) {
                    case Codepoints.FIRE -> runInThread(() -> enterList(ListType.HOT));
                    case Codepoints.UP -> runInThread(() -> enterList(ListType.TOP));
                    case Codepoints.SPARKLES -> runInThread(() -> enterList(ListType.NEW));
                    case Codepoints.IMP -> runInThread(() -> enterList(ListType.CONTRA));
                    case Codepoints.BRUSH -> runInThread(() -> enterList(ListType.MINE));
                }
            } else {
                switch (codepoints) {
                    case Codepoints.LEFT -> runInThread(() -> {
                        page--;
                        populateList();
                    });
                    case Codepoints.RIGHT -> runInThread(() -> {
                        page++;
                        populateList();
                    });
                    case Codepoints.RELOAD -> runInThread(() -> {
                        curList = curListType.getList(getOwner());
                        populateList();
                    });
                    case Codepoints.BACK -> runInThread(() -> {
                        Message message = getMessage();
                        message.editMessageEmbeds(mainEmbed(getOwner())).complete();
                        page = 0;
                        message.clearReactions().complete();
                        mainReactions(message);
                    });
                    default -> {
                        int match = -1;
                        for (int i = 0; i < NUM_CP.length; i++) {
                            if (codepoints.equals(NUM_CP[i])) {
                                match = i;
                                break;
                            }
                        }
                        if (match < 0) return;
                        Suggestion sug = assoc.get(match);
                        if (sug != null) {
                            new SuggestionFlow(channel, sug, getOwner());
                        }
                    }
                }
            }
        }
    }

}
