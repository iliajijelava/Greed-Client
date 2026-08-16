package fun.ogi.util.altmanager;

import fun.ogi.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AccountManager extends CopyOnWriteArrayList<Account> {
    private final AccountFile accountFile;
    private String lastSelected = null;

    public AccountManager() {
        File directory = new File(MinecraftClient.getInstance().runDirectory, "Greed");
        this.accountFile = new AccountFile(new File(directory, "accounts.json"));
        this.accountFile.read(this);
    }

    public void saveLastSelected(String name) {
        this.lastSelected = name;
        this.accountFile.writeLastSelected(this, name);
    }

    public void restoreLastSession() {
        String last = this.accountFile.getLast();
        if (last == null || last.isEmpty()) {
            return;
        }
        this.getAccount(last).ifPresent(account -> {
            try {
                Constructor<Session> constructor = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class, Session.AccountType.class);
                constructor.setAccessible(true);
                Session session = constructor.newInstance(account.name(), UUID.nameUUIDFromBytes(("OfflinePlayer:" + account.name()).getBytes()), MinecraftClient.getInstance().getSession() == null ? "" : MinecraftClient.getInstance().getSession().getAccessToken(), Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
                ((MinecraftClientAccessor) MinecraftClient.getInstance()).setSession(session);
            } catch (Exception ignored) {}
        });
    }

    public AccountFile file() {
        return this.accountFile;
    }

    public void save() {
        this.accountFile.write(this);
    }

    public void addAccount(Account account) {
        if (account == null || this.isAccount(account.name())) {
            return;
        }
        this.add(account);
        this.save();
    }

    public Optional<Account> getAccount(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return this.stream().filter(a -> a.name().equalsIgnoreCase(name)).findFirst();
    }

    public boolean isAccount(String name) {
        return this.getAccount(name).isPresent();
    }

    public void removeAccount(String name) {
        if (name == null) {
            return;
        }
        this.removeIf(a -> a.name().equalsIgnoreCase(name));
        this.save();
    }

    public void clearAccounts() {
        this.clear();
        this.save();
    }

    public List<Account> getFavoriteAccountsSorted() {
        return this.stream().filter(Account::favorite).toList();
    }
}

