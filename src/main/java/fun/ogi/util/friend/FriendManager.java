package fun.ogi.util.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fun.ogi.Cheap;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FriendManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<String> friends = new ArrayList<>();
    private final File file;

    public FriendManager() {
        File dir = new File(Cheap.getInstance().getCheapDir(), "friends.json");
        this.file = dir;
        load();
    }

    public void add(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            save();
        }
    }

    public void remove(String name) {
        friends.remove(name);
        save();
    }

    public boolean contains(String name) {
        return friends.contains(name);
    }

    public void clear() {
        friends.clear();
        save();
    }

    public List<String> getFriends() {
        return friends;
    }

    private void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                friends.addAll(loaded);
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(friends, writer);
        } catch (Exception ignored) {}
    }
}

