package codes.wasabi.suggestbot.events.command;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.events.flow.SuggestFlow;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

public class SuggestCommand extends Command {

    public SuggestCommand() {

    }

    @Override
    protected void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        int limit = Main.db.getSettings().getMaxSuggestionsPerUser();
        if (limit >= 0) {
            int curSuggestions = Main.db.getSuggestionsByOwner(sender.getIdLong()).size();
            int newAmt = curSuggestions + 1;
            if (newAmt > limit) {
                boolean ignore = false;
                if (channel instanceof GuildMessageChannel gmc) {
                    Guild guild = gmc.getGuild();
                    Member member = guild.getMember(sender);
                    if (member != null) ignore = member.hasPermission(Permission.MESSAGE_MANAGE);
                }
                if (!ignore) {
                    stdError(channel, "You can't create any more new suggestions (max is " + limit + ")! Try deleting some.", sender);
                    return;
                }
            }
        }
        new SuggestFlow(channel, sender);
    }

    @Override
    public @NotNull String getName() {
        return "suggest";
    }

}
