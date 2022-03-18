package codes.wasabi.suggestbot.data;

import codes.wasabi.suggestbot.util.ArrayUtil;

public class Settings {

    private int maxSuggestionsPerUser;
    private long[] disallowedChannels;

    Settings(int maxSuggestionsPerUser, long[] disallowedChannels) {
        this.maxSuggestionsPerUser = maxSuggestionsPerUser;
        this.disallowedChannels = disallowedChannels;
    }

    public int getMaxSuggestionsPerUser() {
        return maxSuggestionsPerUser;
    }

    public long[] getDisallowedChannels() {
        return disallowedChannels;
    }

    public void setMaxSuggestionsPerUser(int maxSuggestionsPerUser) {
        this.maxSuggestionsPerUser = maxSuggestionsPerUser;
    }

    public void setDisallowedChannels(long[] disallowedChannels) {
        this.disallowedChannels = disallowedChannels;
    }

    public boolean addDisallowedChannel(long channel) {
        ArrayUtil.ArrayOpResult res = ArrayUtil.addLongToArraySet(disallowedChannels, channel);
        disallowedChannels = res.array();
        return res.succeeded();
    }

    public boolean removeDisallowedChannel(long channel) {
        ArrayUtil.ArrayOpResult res = ArrayUtil.removeLongFromArraySet(disallowedChannels, channel);
        disallowedChannels = res.array();
        return res.succeeded();
    }

}
