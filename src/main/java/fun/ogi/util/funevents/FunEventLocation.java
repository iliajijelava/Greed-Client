package fun.ogi.util.funevents;

public class FunEventLocation {
    private final int x;
    private final int y;
    private final int z;

    public FunEventLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public String asString() {
        return "X: " + x + " Y: " + y + " Z: " + z;
    }

    @Override
    public String toString() {
        return asString();
    }
}

