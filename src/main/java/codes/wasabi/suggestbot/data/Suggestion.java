package codes.wasabi.suggestbot.data;

import codes.wasabi.suggestbot.util.ArrayUtil;

public class Suggestion {

    private final Category category;
    private final long owner;
    private String content;
    private long[] upvotes;
    private long[] downvotes;
    private final long creationTime;

    Suggestion(Category category, long owner, String content, long[] upvotes, long[] downvotes, long creationTime) {
        this.category = category;
        this.owner = owner;
        this.content = content;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.creationTime = creationTime;
    }

    public Category getCategory() {
        return category;
    }

    public long getOwner() {
        return owner;
    }

    public String getContent() {
        return content;
    }

    public long[] getUpVotes() {
        return upvotes;
    }

    public long[] getDownVotes() {
        return downvotes;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getVoteCount() {
        return upvotes.length - downvotes.length;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUpVotes(long[] upvotes) {
        this.upvotes = upvotes;
    }

    public void setDownVotes(long[] downvotes) {
        this.downvotes = downvotes;
    }

    public void clearVote(long user) {
        removeUpvote(user);
        removeDownvote(user);
    }

    public boolean addUpvote(long user) {
        clearVote(user);
        ArrayUtil.ArrayOpResult res = ArrayUtil.addLongToArraySet(upvotes, user);
        upvotes = res.array();
        return res.succeeded();
    }

    public boolean removeUpvote(long user) {
        ArrayUtil.ArrayOpResult res = ArrayUtil.removeLongFromArraySet(upvotes, user);
        upvotes = res.array();
        return res.succeeded();
    }

    public boolean addDownvote(long user) {
        clearVote(user);
        ArrayUtil.ArrayOpResult res = ArrayUtil.addLongToArraySet(downvotes, user);
        downvotes = res.array();
        return res.succeeded();
    }

    public boolean removeDownvote(long user) {
        ArrayUtil.ArrayOpResult res = ArrayUtil.removeLongFromArraySet(downvotes, user);
        downvotes = res.array();
        return res.succeeded();
    }

    // must be up for at least 15 minutes in order to qualify as "hot"
    private static final long minUptime = 60000L * 15L;
    private static final double msToDays = (1 / 1000d) * (1 / 60d) * (1 / 60d) * (1 / 24d);
    public double getHeat() {
        long now = System.currentTimeMillis();
        long elapsed = now - creationTime;
        if (elapsed < minUptime) return 0d;
        // heat is "votes per day"
        double days = elapsed * msToDays;
        return getVoteCount() / days;
    }

}
