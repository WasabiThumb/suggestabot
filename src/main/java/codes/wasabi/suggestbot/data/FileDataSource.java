package codes.wasabi.suggestbot.data;

import codes.wasabi.suggestbot.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class FileDataSource extends StreamDataSource {

    private final File file;

    public FileDataSource(@NotNull File file) {
        this.file = file;
    }

    private void validate() throws IOException {
        if (!file.exists()) {
            if (file.createNewFile()) Logger.INFO.printLine("Created new empty file " + file);
        }
    }

    @Override
    public @NotNull InputStream getInputStream() throws Exception {
        validate();
        return new FileInputStream(file);
    }

    @Override
    public @NotNull OutputStream getOutputStream() throws Exception {
        if (file.delete()) Logger.INFO.printLine("Overwriting file " + file);
        validate();
        return new FileOutputStream(file, false);
    }

}
