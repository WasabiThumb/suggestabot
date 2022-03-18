package codes.wasabi.suggestbot.events.command;

import codes.wasabi.suggestbot.events.flow.ConfigFlow;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

public class ConfigCommand extends Command {

    public ConfigCommand() {

    }

    @Override
    protected void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        if (channel instanceof GuildMessageChannel gmc) {
            Guild guild = gmc.getGuild();
            Member member = guild.getMember(sender);
            if (member != null) {
                if (member.hasPermission(Permission.MESSAGE_MANAGE)) {
                    new ConfigFlow(channel, sender);
                    return;
                }
            }
        }
        stdError(channel, "You can't use this command! (Must be in a guild and have the Manage Messages permission)", sender);
    }

    @Override
    public @NotNull String getName() {
        return "config";
    }

}
