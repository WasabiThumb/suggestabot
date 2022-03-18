package codes.wasabi.suggestbot.events.command;

import codes.wasabi.suggestbot.Main;
import codes.wasabi.suggestbot.logging.Logger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class Command {

    private static final Map<String, Command> registry = new HashMap<>();

    public static final Command NONE = new Command() {
        @Override
        protected void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) { }

        @Override
        public @NotNull String getName() {
            return "NONE";
        }
    };

    public static @Nullable Command get(@NotNull String name) {
        return registry.get(name.toLowerCase(Locale.ROOT));
    }

    public static void loadDefaults() {
        Logger.INFO.printLine("Loading default commands");
        registry.clear();
        Reflections reflections = new Reflections("codes.wasabi.suggestbot.events.command");
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);
        int total = 0;
        int loaded = 0;
        for (Class<? extends Command> clazz : classes) {
            if (clazz.isInterface()) continue;
            if ((clazz.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT) continue;
            if (clazz == NONE.getClass()) continue;
            total++;
            try {
                Constructor<? extends Command> constructor = clazz.getConstructor();
                Command cmd = constructor.newInstance();
                String name = cmd.getName().toLowerCase(Locale.ROOT);
                Command existing = registry.get(name);
                if (existing != null) {
                    if (cmd.getPriority() > existing.getPriority()) registry.put(name, cmd);
                } else {
                    registry.put(name, cmd);
                }
                loaded++;
            } catch (SecurityException | ReflectiveOperationException | IllegalArgumentException | ExceptionInInitializerError e) {
                Logger.WARN.printLine("Failed to load command " + clazz.getName());
                Logger.WARN.printLine(e);
            }
        }
        Logger.INFO.printLine("Loaded " + loaded + "/" + total + " commands successfully");
    }

    public static boolean execute(@NotNull String cmdName, @NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        Command cmd = get(cmdName);
        if (cmd == null) {
            Command.NONE.stdError(channel, "No such command named \"" + cmdName + "\"", sender);
            return false;
        } else {
            if (channel instanceof GuildChannel gc) {
                Guild guild = gc.getGuild();
                Member member = guild.getMember(sender);
                if (member == null) return false;
                boolean privileged = member.hasPermission(Permission.MESSAGE_MANAGE);
                if (!privileged) {
                    if (!cmd.allowInGuilds()) {
                        Command.NONE.stdError(channel, "This command is only allowed in DMs!", sender);
                        return false;
                    }
                    long guildId = guild.getIdLong();
                    long[] disallowed = Main.db.getSettings().getDisallowedChannels();
                    for (long l : disallowed) {
                        if (l == guildId) {
                            Command.NONE.stdError(channel, "Commands are disabled in this channel!", sender);
                            return false;
                        }
                    }
                }
            }
            try {
                cmd.execute(channel, sender, args);
            } catch (Exception e) {
                Command.NONE.stdError(channel, "An unknown error occurred while executing this command (" + e.getClass().getName() + ")", sender);
                Logger.ERROR.printLine(e);
                return false;
            }
            return true;
        }
    }

    protected abstract void onExecute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args);

    public final void execute(@NotNull MessageChannel channel, @NotNull User sender, @NotNull String[] args) {
        int minArgs = getMinArgs();
        int maxArgs = getMaxArgs();
        boolean same = minArgs == maxArgs;
        if (args.length < minArgs) {
            stdError(channel, "Not enough arguments (requires " + (same ? "" : "at least ") + minArgs + "!)", sender);
            return;
        }
        if (args.length > maxArgs) {
            stdError(channel, "Too many arguments (requires " + (same ? "" : "less than ") + maxArgs + "!)", sender);
            return;
        }
        onExecute(channel, sender, args);
    }

    public abstract @NotNull String getName();

    public int getPriority() {
        return 1;
    }

    public int getMinArgs() {
        return 0;
    }

    public int getMaxArgs() {
        return Integer.MAX_VALUE;
    }

    public boolean allowInGuilds() {
        return true;
    }

    private void stdOut(@NotNull MessageChannel channel, @NotNull String message, @Nullable User reply, @NotNull String typeName, @NotNull Color color) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(color);
        builder.setTitle(typeName);
        builder.setDescription(message);
        if (reply != null) {
            try {
                String name = "@" + reply.getName() + "#" + reply.getDiscriminator();
                builder.setFooter("Requested by " + name, reply.getEffectiveAvatarUrl());
            } catch (Exception ignored) { }
        }
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    protected final void stdInfo(@NotNull MessageChannel channel, @NotNull String message, @Nullable User reply) {
        stdOut(channel, message, reply, "Info", Color.LIGHT_GRAY);
    }

    protected final void stdWarn(@NotNull MessageChannel channel, @NotNull String message, @Nullable User reply) {
        stdOut(channel, message, reply, "Warning", Color.YELLOW);
    }

    protected final void stdError(@NotNull MessageChannel channel, @NotNull String message, @Nullable User reply) {
        stdOut(channel, message, reply, "Error", Color.RED);
    }

    protected final void stdSuccess(@NotNull MessageChannel channel, @NotNull String message, @Nullable User reply) {
        stdOut(channel, message, reply, "Success", Color.GREEN);
    }

}
