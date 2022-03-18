package codes.wasabi.suggestbot.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class StreamDataSource implements DataSource {

    private static final byte[] _HEADER = "sB@".getBytes(StandardCharsets.UTF_8);

    private Settings settings = new Settings(-1, new long[0]);
    private Collection<Category> categories = new ArrayList<>();
    private Collection<Suggestion> suggestions = new ArrayList<>();

    public abstract @NotNull InputStream getInputStream() throws Exception;
    public abstract @NotNull OutputStream getOutputStream() throws Exception;
    public boolean shouldCloseInputStream() {
        return true;
    }
    public boolean shouldCloseOutputStream() {
        return true;
    }

    @Override
    public void load() throws Exception {
        InputStream is = getInputStream();
        byte[] read = is.readNBytes(_HEADER.length);
        for (int i=0; i < read.length; i++) {
            if (read[i] != _HEADER[i]) throw new IOException("Malformed header! (Expected " + _HEADER[i] + " in position " + i + ", got " + read[i] + ")");
        }
        int len;
        // settings block
        int max = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
        len = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
        long[] channels = new long[len];
        for (int i=0; i < len; i++) {
            channels[i] = ByteBuffer.wrap(is.readNBytes(Long.BYTES)).getLong();
        }
        Settings _settings = new Settings(max, channels);
        // category block
        len = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
        Map<String, Category> _categories = new HashMap<>();
        while (len > 0) {
            int strBytes = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
            String name = new String(is.readNBytes(strBytes), StandardCharsets.UTF_8);
            boolean locked = is.readNBytes(1)[0] == ((byte) 1);
            _categories.put(name, new Category(name, locked));
            len--;
        }
        // suggestion block
        len = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
        List<Suggestion> _suggestions = new ArrayList<>();
        while (len > 0) {
            int strBytes;
            strBytes = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
            String catName = new String(is.readNBytes(strBytes), StandardCharsets.UTF_8);
            Category category = _categories.get(catName);
            if (category == null) {
                len--;
                continue;
            }
            long owner = ByteBuffer.wrap(is.readNBytes(Long.BYTES)).getLong();
            strBytes = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
            String content = new String(is.readNBytes(strBytes), StandardCharsets.UTF_8);
            int voteCount;
            voteCount = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
            long[] upvotes = new long[voteCount];
            for (int i=0; i < voteCount; i++) {
                upvotes[i] = ByteBuffer.wrap(is.readNBytes(Long.BYTES)).getLong();
            }
            voteCount = ByteBuffer.wrap(is.readNBytes(Integer.BYTES)).getInt();
            long[] downvotes = new long[voteCount];
            for (int i=0; i < voteCount; i++) {
                downvotes[i] = ByteBuffer.wrap(is.readNBytes(Long.BYTES)).getLong();
            }
            long creationTime = ByteBuffer.wrap(is.readNBytes(Long.BYTES)).getLong();
            _suggestions.add(new Suggestion(category, owner, content, upvotes, downvotes, creationTime));
            len--;
        }
        // apply
        settings = _settings;
        categories = new ArrayList<>(_categories.values());
        suggestions = _suggestions;
        if (shouldCloseInputStream()) {
            is.close();
        }
    }

    @Override
    public void save() throws Exception {
        OutputStream os = getOutputStream();
        os.write(_HEADER);
        // settings block
        int max = settings.getMaxSuggestionsPerUser();
        os.write(ByteBuffer.allocate(Integer.BYTES).putInt(max).array());
        long[] channels = settings.getDisallowedChannels();
        os.write(ByteBuffer.allocate(Integer.BYTES).putInt(channels.length).array());
        for (long l : channels) {
            os.write(ByteBuffer.allocate(Long.BYTES).putLong(l).array());
        }
        // category block
        os.write(ByteBuffer.allocate(Integer.BYTES).putInt(categories.size()).array());
        for (Category cat : categories) {
            String name = cat.getName();
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            os.write(ByteBuffer.allocate(Integer.BYTES).putInt(nameBytes.length).array());
            os.write(nameBytes);
            os.write(cat.isLocked() ? ((byte) 1) : ((byte) 0));
        }
        // suggestion block
        os.write(ByteBuffer.allocate(Integer.BYTES).putInt(suggestions.size()).array());
        for (Suggestion sug : suggestions) {
            String catName = sug.getCategory().getName();
            byte[] bytes = catName.getBytes(StandardCharsets.UTF_8);
            os.write(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            os.write(bytes);
            os.write(ByteBuffer.allocate(Long.BYTES).putLong(sug.getOwner()).array());
            bytes = sug.getContent().getBytes(StandardCharsets.UTF_8);
            os.write(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            os.write(bytes);
            long[] votes = sug.getUpVotes();
            for (int i=0; i < 2; i++) {
                os.write(ByteBuffer.allocate(Integer.BYTES).putInt(votes.length).array());
                for (long l : votes) {
                    os.write(ByteBuffer.allocate(Long.BYTES).putLong(l).array());
                }
                votes = sug.getDownVotes();
            }
            os.write(ByteBuffer.allocate(Long.BYTES).putLong(sug.getCreationTime()).array());
        }
        if (shouldCloseOutputStream()) os.close();
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    @Override
    public Collection<Category> getCategories() {
        return categories;
    }

    @Override
    public Collection<Suggestion> getSuggestions() {
        return suggestions;
    }

}
