package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlacklistFlow extends ListFlow {

    private List<MessageEmbed.Field> list;

    public BlacklistFlow(@NotNull MessageChannel channel, @NotNull User owner) {
        super(channel, "Blacklisted Channels", owner);
    }

    private void generateList() {
        list = new ArrayList<>();
        int i = 0;
        for (long l : Main.db.getSettings().getDisallowedChannels()) {
            GuildChannel gc = Main.jda.getGuildChannelById(l);
            if (gc == null) continue;
            i++;
            list.add(new MessageEmbed.Field(Integer.toString(i), gc.getAsMention(), false));
        }
    }

    @Override
    protected List<MessageEmbed.Field> getList() {
        return list;
    }

    @Override
    protected void markStale() {
        generateList();
    }

}
