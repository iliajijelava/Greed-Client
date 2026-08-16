package fun.ogi.util.staff;

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

public class StaffManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<String> staff = new ArrayList<>();
    private final File file;

    public StaffManager() {
        this.file = new File(Cheap.getInstance().getCheapDir(), "staff.json");
        load();
    }

    public void add(String name) {
        if (!staff.contains(name)) {
            staff.add(name);
            save();
        }
    }

    public void remove(String name) {
        staff.remove(name);
        save();
    }

    public boolean contains(String name) {
        return staff.contains(name);
    }

    public void clear() {
        staff.clear();
        save();
    }

    public List<String> getStaff() {
        return staff;
    }

    private void load() {
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                staff.addAll(loaded);
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(staff, writer);
        } catch (Exception ignored) {}
    }
}

