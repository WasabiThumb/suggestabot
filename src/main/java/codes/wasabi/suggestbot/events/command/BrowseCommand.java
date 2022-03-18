package codes.wasabi.suggestbot.events.command;

import codes.wasabi.suggestbot.events.flow.BrowseFlow;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

public class BrowseCommand extends Command {

    public BrowseCommand() {

    }

    @Override
    protected void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        new BrowseFlow(channel, sender);
    }

    @Override
    public @NotNull String getName() {
        return "browse";
    }

}
