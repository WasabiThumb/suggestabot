package codes.wasabi.suggestbot.events.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class HelpCommand extends Command {

    public HelpCommand() {

    }

    @Override
    protected void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        channel.sendMessageEmbeds(
                (new EmbedBuilder())
                        .setColor(Color.BLUE)
                        .setTitle("Help")
                        .setDescription("Suggestibot v1.0.0 by Wasabi")
                        .addField("help", "Shows this screen", false)
                        .addField("config", "Configures the bot (admin only)", false)
                        .addField("suggest", "Create a new suggestion", false)
                        .addField("browse", "Browse through suggestions", false)
                        .setFooter("Requested by @" + sender.getName() + "#" + sender.getDiscriminator(), sender.getEffectiveAvatarUrl())
                        .build()
        ).queue();
    }

    @Override
    public @NotNull String getName() {
        return "help";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

}
