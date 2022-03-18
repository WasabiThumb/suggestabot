package codes.wasabi.suggestbot;

import codes.wasabi.suggestbot.data.DataSource;
import codes.wasabi.suggestbot.data.FileDataSource;
import codes.wasabi.suggestbot.events.Events;
import codes.wasabi.suggestbot.events.command.Command;
import codes.wasabi.suggestbot.events.flow.Flow;
import codes.wasabi.suggestbot.logging.Logger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Main {

    public static DataSource db;
    public static JDA jda;

    public static void main(String[] args) {
        AppDirs appDirs = AppDirsFactory.getInstance();
        String dir = appDirs.getUserDataDir("SuggestBot", null, "Wasabi Codes", true);
        Logger.INFO.printLine("Set data directory to " + dir);
        File f = new File(dir);
        boolean attemptLoad = true;
        if (f.mkdirs()) {
            Logger.INFO.printLine("Created data directory successfully");
            attemptLoad = false;
        }
        File dbFile = new File(f, "data.bin");
        attemptLoad = attemptLoad && dbFile.exists();
        db = new FileDataSource(dbFile);
        if (attemptLoad) {
            Logger.INFO.printLine("Attempting to load save data (if any exists)");
            try {
                db.load();
            } catch (Exception e) {
                Logger.WARN.printLine("Failed to load save data (corrupted or nonexistent)?");
                Logger.WARN.printLine(e);
            }
        }
        Command.loadDefaults();
        Logger.INFO.printLine("Starting flow timeout thread");
        Flow.Manager.startTimeoutThread();
        JDABuilder builder = JDABuilder.createDefault(args[0]);
        builder.disableCache(CacheFlag.ACTIVITY);
        builder.setActivity(Activity.watching("for >]help"));
        builder.addEventListeners(new Events());
        JDA jda;
        try {
            jda = builder.build();
            Main.jda = jda;
            jda.awaitReady();
        } catch (LoginException | IllegalArgumentException | InterruptedException e) {
            Logger.ERROR.printLine(e);
            System.exit(1);
        }
        Thread thread = new Thread(() -> {
            boolean repeat = true;
            while (repeat) {
                try {
                    TimeUnit.SECONDS.sleep(60L);
                } catch (InterruptedException e) {
                    repeat = false;
                }
                Logger.INFO.printLine("Autosaving...");
                try {
                    db.save();
                    Logger.INFO.printLine("Saved!");
                } catch (Exception e) {
                    Logger.ERROR.printLine("Encountered error while trying to save data");
                    Logger.ERROR.printLine(e);
                }
            }
        });
        thread.setName("Autosave Thread");
        thread.start();
    }

}
