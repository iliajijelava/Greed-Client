package fun.ogi.util.storages.waypoint;

import java.util.Objects;

public class Waypoint {
    private final String name;
    private final int x;
    private final int y;
    private final int z;

    public Waypoint(String name, int x, int y, int z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Waypoint w)) return false;
        return Objects.equals(name, w.name) && x == w.x && y == w.y && z == w.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y, z);
    }
}

