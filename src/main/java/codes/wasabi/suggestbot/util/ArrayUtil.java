package codes.wasabi.suggestbot.util;

import org.jetbrains.annotations.NotNull;

public class ArrayUtil {

    public record ArrayOpResult(long[] array, boolean succeeded) {}

    public static @NotNull ArrayOpResult addLongToArraySet(long[] array, long value) {
        long[] ret = new long[array.length + 1];
        int i = 0;
        for (long l : array) {
            if (l == value) return new ArrayOpResult(array, false);
            ret[i] = l;
            i++;
        }
        ret[i] = value;
        return new ArrayOpResult(ret, true);
    }

    public static ArrayOpResult removeLongFromArraySet(long[] array, long value) {
        int len = array.length;
        if (len == 0) return new ArrayOpResult(array, false);
        len -= 1;
        long[] ret = new long[len];
        boolean removed = false;
        int i = 0;
        for (long l : array) {
            if (l == value) {
                removed = true;
                continue;
            }
            if (i >= len) break;
            ret[i] = l;
            i++;
        }
        if (removed) {
            return new ArrayOpResult(ret, true);
        }
        return new ArrayOpResult(array, false);
    }

}
