package fun.ogi.util.altmanager;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Account {
    private final LocalDateTime creationDate;
    private final String name;
    private boolean favorite;

    public Account(LocalDateTime creationDate, String name) {
        this.creationDate = Objects.requireNonNull(creationDate, "creationDate");
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }
    }

    public LocalDateTime creationDate() {
        return this.creationDate;
    }

    public String name() {
        return this.name;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public void favorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void toggleFavorite() {
        this.favorite = !this.favorite;
    }
}

