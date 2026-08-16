package fun.ogi.util.altmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class MainGenerator {
    private static final Random RANDOM = new Random();
    private static final List<String> NICKS = buildNickPool();

    private MainGenerator() {}

    public static String generate() {
        String base = NICKS.get(RANDOM.nextInt(NICKS.size()));
        if (RANDOM.nextBoolean()) {
            int digits = RANDOM.nextInt(1, 5);
            int maxNumber = (int) Math.pow(10.0, digits);
            int minNumber = digits == 1 ? 0 : (int) Math.pow(10.0, digits - 1);
            base = base + RANDOM.nextInt(minNumber, maxNumber);
        }
        if (base.length() > 16) {
            base = base.substring(0, 16);
        }
        return base;
    }

    private static List<String> buildNickPool() {
        String[] roots = {
            "arv", "brel", "civ", "drax", "elv",
            "fyn", "gorv", "hyr", "ivor", "jex",
            "kiv", "lorn", "morv", "nyx", "orv",
            "quiv", "rath", "sylv", "torv", "ulv",
            "vex", "wryn", "xev", "zyr", "varn"
        };
        String[] tails = {
            "ael", "anor", "bryn", "cora", "dune",
            "elyx", "faro", "glen", "hush", "iris",
            "jora", "kael", "lume", "mora", "nex",
            "oris", "phel", "quor", "riven", "sol",
            "tiv", "ulen", "voro", "wyr", "xen",
            "yra", "zeth", "nix", "shade", "moor",
            "flux", "drift", "spark", "hollow", "echo",
            "ember", "glint", "mist", "rune", "vail"
        };
        List<String> nicks = new ArrayList<>(roots.length * tails.length);
        for (String root : roots) {
            for (String tail : tails) {
                nicks.add(root + tail);
            }
        }
        return List.copyOf(nicks);
    }
}

