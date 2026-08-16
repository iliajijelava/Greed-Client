package fun.ogi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BossBarData {
    private static volatile List<String> bossBarTexts = Collections.emptyList();

    public static void updateBossBars(List<String> texts) {
        bossBarTexts = Collections.unmodifiableList(new ArrayList<>(texts));
    }

    public static List<String> getBossBarTexts() {
        return bossBarTexts;
    }

    public static boolean containsText(String keyword) {
        String lower = keyword.toLowerCase();
        for (String text : bossBarTexts) {
            if (text.toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty() {
        return bossBarTexts.isEmpty();
    }
}

