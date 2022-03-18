package codes.wasabi.suggestbot.util;

import org.jetbrains.annotations.NotNull;

public final class PeriodFormatter {

    private static final double SEC = 1000d;
    private static final double MIN = SEC * 60d;
    private static final double HOUR = MIN * 60d;
    private static final double DAY = HOUR * 24d;
    private static final double MONTH = DAY * 30.4375d;
    private static final double YEAR = DAY * 365.25d;

    private static final double TWO_SEC = SEC * 2d;
    private static final double TWO_MIN = MIN * 2d;
    private static final double TWO_HOUR = HOUR * 2d;
    private static final double TWO_DAY = DAY * 2d;
    private static final double TWO_MONTH = MONTH * 2d;
    private static final double TWO_YEAR = YEAR * 2d;
    public static @NotNull String format(long millis) {
        if (millis < SEC) return "just now";
        if (millis < MIN) return (int) Math.floor(millis / SEC) + " second" + (millis < TWO_SEC ? "" : "s") + " ago";
        if (millis < HOUR) return (int) Math.floor(millis / MIN) + " minute" + (millis < TWO_MIN ? "" : "s") + " ago";
        if (millis < DAY) return (int) Math.floor(millis / HOUR) + " hour" + (millis < TWO_HOUR ? "" : "s") + " ago";
        if (millis < MONTH) return (int) Math.floor(millis / DAY) + " day" + (millis < TWO_DAY ? "" : "s") + " ago";
        if (millis < YEAR) return (int) Math.floor(millis / MONTH) + " month" + (millis < TWO_MONTH ? "" : "s") + " ago";
        return (int) Math.floor(millis / YEAR) + " year" + (millis < TWO_YEAR ? "" : "s") + " ago";
    }

}
