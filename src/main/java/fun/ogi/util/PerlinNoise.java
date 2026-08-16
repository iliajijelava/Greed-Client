package fun.ogi.util;

public class PerlinNoise {

    private static final int[] PERM = new int[512];
    private static final int[][] GRAD3 = {
            {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
            {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
            {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}
    };

    private final int[] perm;

    public PerlinNoise() {
        this(System.nanoTime());
    }

    public PerlinNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        long s = seed;
        for (int i = 255; i > 0; i--) {
            s = (s * 6364136223846793005L + 1442695040888963407L) & 0x7FFFFFFFFFFFFFFFL;
            int j = (int) (s % (i + 1));
            if (j < 0) j += (i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        perm = new int[512];
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    private static double dot(int[] g, double x, double y, double z) {
        return g[0] * x + g[1] * y + g[2] * z;
    }

    public double noise(double x) {
        return noise(x, 0);
    }

    public double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];

        return lerp(v,
                lerp(u, dot(GRAD3[aa % 12], xf, yf), dot(GRAD3[ba % 12], xf - 1, yf)),
                lerp(u, dot(GRAD3[ab % 12], xf, yf - 1), dot(GRAD3[bb % 12], xf - 1, yf - 1))
        );
    }

    public double noise(double x, double y, double z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double zf = z - Math.floor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int aaa = perm[perm[perm[xi] + yi] + zi];
        int aba = perm[perm[perm[xi] + yi + 1] + zi];
        int aab = perm[perm[perm[xi] + yi] + zi + 1];
        int abb = perm[perm[perm[xi] + yi + 1] + zi + 1];
        int baa = perm[perm[perm[xi + 1] + yi] + zi];
        int bba = perm[perm[perm[xi + 1] + yi + 1] + zi];
        int bab = perm[perm[perm[xi + 1] + yi] + zi + 1];
        int bbb = perm[perm[perm[xi + 1] + yi + 1] + zi + 1];

        return lerp(w,
                lerp(v,
                        lerp(u, dot(GRAD3[aaa % 12], xf, yf, zf), dot(GRAD3[baa % 12], xf - 1, yf, zf)),
                        lerp(u, dot(GRAD3[aba % 12], xf, yf - 1, zf), dot(GRAD3[bba % 12], xf - 1, yf - 1, zf))
                ),
                lerp(v,
                        lerp(u, dot(GRAD3[aab % 12], xf, yf, zf - 1), dot(GRAD3[bab % 12], xf - 1, yf, zf - 1)),
                        lerp(u, dot(GRAD3[abb % 12], xf, yf - 1, zf - 1), dot(GRAD3[bbb % 12], xf - 1, yf - 1, zf - 1))
                )
        );
    }
}

