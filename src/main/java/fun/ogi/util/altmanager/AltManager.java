package fun.ogi.util.altmanager;

import java.time.LocalDateTime;
import java.util.List;

public class AltManager {
    public static final AccountManager MANAGER = new AccountManager();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;
        MANAGER.restoreLastSession();
    }

    public static List<String> getAccounts() {
        return MANAGER.stream().map(Account::name).toList();
    }

    public static void addAccount(String username) {
        if (!MANAGER.isAccount(username)) {
            MANAGER.addAccount(new Account(LocalDateTime.now(), username));
        }
    }

    public static void removeAccount(String username) {
        MANAGER.removeAccount(username);
    }
}

