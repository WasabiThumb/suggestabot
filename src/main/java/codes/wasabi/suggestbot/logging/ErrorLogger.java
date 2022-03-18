package codes.wasabi.suggestbot.logging;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

public class ErrorLogger implements Logger {

    private static final Ansi root = Ansi.ansi().fgBright(Ansi.Color.BLACK).a("[").fg(Ansi.Color.RED).a("ERROR").fgBright(Ansi.Color.BLACK).a("] ").reset();

    @Override
    public void print(@NotNull Ansi ansi) {
        System.out.print(Ansi.ansi().a(root).a(ansi));
    }

}
