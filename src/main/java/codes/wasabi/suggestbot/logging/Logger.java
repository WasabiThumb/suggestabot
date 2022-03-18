package codes.wasabi.suggestbot.logging;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public interface Logger {

    Logger INFO = new InfoLogger();
    Logger WARN = new WarnLogger();
    Logger ERROR = new ErrorLogger();

    void print(@NotNull Ansi ansi);

    default void printLine(@NotNull Ansi ansi) {
        print(Ansi.ansi().a(ansi).a("\n"));
    }

    default void print(@NotNull String string) {
        print(Ansi.ansi().a(string));
    }

    default void printLine(@NotNull String string) {
        print(Ansi.ansi().a(string).a("\n"));
    }

    default void print(@NotNull Object object) {
        print(object.toString());
    }

    default void printLine(@NotNull Object object) {
        printLine(object.toString());
    }

    private static String throwableToString(@NotNull Throwable t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos, false, StandardCharsets.UTF_8);
        t.printStackTrace(ps);
        ps.flush();
        return bos.toString(StandardCharsets.UTF_8);
    }

    default void print(@NotNull Throwable throwable) {
        print(throwableToString(throwable));
    }

    default void printLine(@NotNull Throwable throwable) {
        printLine(throwableToString(throwable));
    }

}
