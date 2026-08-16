package fun.ogi.util.macro;

import java.util.ArrayList;
import java.util.List;

public class MacroManager {
    private static final List<Macro> macros = new ArrayList<>();

    public static List<Macro> getMacros() {
        return macros;
    }

    public static void add(Macro macro) {
        macros.removeIf(m -> m.getCommand().equalsIgnoreCase(macro.getCommand()));
        macros.add(macro);
    }

    public static boolean removeByCommand(String command) {
        return macros.removeIf(m -> m.getCommand().equalsIgnoreCase(command));
    }

    public static void clear() {
        macros.clear();
    }

    public static boolean isEmpty() {
        return macros.isEmpty();
    }

    public static Macro getByKey(int key) {
        for (Macro macro : macros) {
            if (macro.getKey() == key) return macro;
        }
        return null;
    }
}

