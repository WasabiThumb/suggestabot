package codes.wasabi.suggestbot.data;

public class Category {

    private String name;
    private boolean locked;

    Category(String name, boolean locked) {
        this.name = name;
        this.locked = locked;
    }

    public String getName() {
        return name;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

}
