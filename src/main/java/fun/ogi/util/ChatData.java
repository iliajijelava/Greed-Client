package fun.ogi.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatData {
    private static volatile List<String> recentMessages = new ArrayList<>();
    private static final int MAX_MESSAGES = 50;

    public static void addMessage(String text) {
        List<String> updated = new ArrayList<>(recentMessages);
        updated.add(text);
        if (updated.size() > MAX_MESSAGES) {
            updated.remove(0);
        }
        recentMessages = Collections.unmodifiableList(updated);
    }

    public static List<String> getRecentMessages() {
        return recentMessages;
    }

    public static boolean containsRecently(String keyword, int withinLastN) {
        String lower = keyword.toLowerCase();
        List<String> msgs = recentMessages;
        int start = Math.max(0, msgs.size() - withinLastN);
        for (int i = start; i < msgs.size(); i++) {
            if (msgs.get(i).toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        recentMessages = Collections.emptyList();
    }
}

