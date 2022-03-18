package codes.wasabi.suggestbot.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public interface DataSource {

    @Contract(value = " -> !null", pure = true)
    Settings getSettings();

    @Contract(value = " -> !null", pure = true)
    Collection<Category> getCategories();

    @Contract(value = " -> !null", pure = true)
    Collection<Suggestion> getSuggestions();

    default @Nullable Category getCategory(@NotNull String name) {
        for (Category cat : getCategories()) {
            if (cat.getName().equalsIgnoreCase(name)) return cat;
        }
        return null;
    }

    default boolean removeCategory(@NotNull String name) {
        Collection<Category> cats = getCategories();
        for (Category cat : cats) {
            if (cat.getName().equalsIgnoreCase(name)) {
                cats.remove(cat);
                getSuggestions().removeIf(sug -> sug.getCategory() == cat);
                return true;
            }
        }
        return false;
    }

    default @Nullable Category createCategory(@NotNull String name) {
        return createCategory(name, false);
    }

    default @Nullable Category createCategory(@NotNull String name, boolean locked) {
        Collection<Category> collection = getCategories();
        for (Category c : getCategories()) {
            if (c.getName().equalsIgnoreCase(name)) {
                return null;
            }
        }
        Category cat = new Category(name, locked);
        collection.add(cat);
        return cat;
    }

    default @NotNull Category getOrCreateCategory(@NotNull String name) {
        return getOrCreateCategory(name, false);
    }

    default @NotNull Category getOrCreateCategory(@NotNull String name, boolean locked) {
        Category ret;
        ret = getCategory(name);
        if (ret != null) return ret;
        ret = createCategory(name, locked);
        assert (ret != null);
        return ret;
    }

    default @NotNull Suggestion addSuggestion(@NotNull Category category, long owner, @NotNull String content) {
        Suggestion newSuggestion = new Suggestion(category, owner, content, new long[] { owner }, new long[0], System.currentTimeMillis());
        getSuggestions().add(newSuggestion);
        return newSuggestion;
    }

    default @NotNull Collection<Suggestion> getSuggestionsByOwner(long owner) {
        return getSuggestions().stream().filter((Suggestion sug) -> sug.getOwner() == owner).collect(Collectors.toCollection(ArrayList::new));
    }

    default @NotNull Collection<Suggestion> getSuggestionsByCategory(@NotNull Category category) {
        return getSuggestions().stream().filter((Suggestion sug) -> sug.getCategory() == category).collect(Collectors.toCollection(ArrayList::new));
    }

    void load() throws Exception;

    void save() throws Exception;

}
