package codes.wasabi.suggestbot.events.flow;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.data.Category;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CategoryFlow extends ListFlow {

    private List<MessageEmbed.Field> list;

    public CategoryFlow(@NotNull MessageChannel channel, @NotNull User owner) {
        super(channel, "Suggestion Categories", owner);
    }

    @Override
    protected List<MessageEmbed.Field> getList() {
        return list;
    }

    @Override
    protected void markStale() {
        list = new ArrayList<>();
        int i = 0;
        for (Category cat : Main.db.getCategories()) {
            i++;
            list.add(new MessageEmbed.Field(Integer.toString(i), cat.getName() + (cat.isLocked() ? " \uD83D\uDD12" : ""), false));
        }
    }

}
