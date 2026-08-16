package fun.ogi.rotation.ainew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.client.MinecraftClient;

public final class AINewRotationManager {
    public static final int DEFAULT_TRAINING_DURATION_SEC = 60;
    public static final int MIN_TRAINING_DURATION_SEC = 15;
    public static final int MAX_TRAINING_DURATION_SEC = 300;
    private static final long BLOATED_FILE_BYTES = 3000000L;
    private final Gson gson = new GsonBuilder().create();
    private final List<AINewRotationProfile> profiles = new ArrayList<AINewRotationProfile>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();
    private boolean loaded;
    private int trainingDurationSec = 60;
    private AINewCombatTrainingSession activeTraining;
    private String lastSavedProfileName;

    public void ensureLoaded() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        Path path = AINewRotationManager.getPath();
        try {
            if (!Files.exists(path, new LinkOption[0])) {
                return;
            }
            long fileSize = Files.size(path);
            if (fileSize > 3000000L) {
                Path backup = path.resolveSibling(path.getFileName().toString() + ".bak");
                Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[AINew] AI profile file was too large (" + fileSize + " bytes) and was moved to " + backup.getFileName());
                return;
            }
            JsonObject root = (JsonObject)this.gson.fromJson(Files.readString(path), JsonObject.class);
            if (root == null) {
                return;
            }
            if (root.has("trainingDurationSec")) {
                this.trainingDurationSec = AINewRotationManager.clampTrainingDuration(root.get("trainingDurationSec").getAsInt());
            }
            if (!root.has("profiles") || !root.get("profiles").isJsonArray()) {
                return;
            }
            this.profiles.clear();
            JsonArray array = root.getAsJsonArray("profiles");
            for (int i = 0; i < array.size(); ++i) {
                AINewRotationProfile profile;
                if (!array.get(i).isJsonObject() || (profile = AINewRotationProfile.fromJson(array.get(i).getAsJsonObject())) == null || this.getProfile(profile.getName()) != null) continue;
                this.profiles.add(profile);
            }
            this.profiles.sort(Comparator.comparing(AINewRotationProfile::getName, String.CASE_INSENSITIVE_ORDER));
        }
        catch (Exception e) {
            System.err.println("[AINew] Failed to load AI new rotations");
            e.printStackTrace();
        }
    }

    public void save() {
        this.ensureLoaded();
        Path path = AINewRotationManager.getPath();
        try {
            Files.createDirectories(path.getParent(), new FileAttribute[0]);
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (AINewRotationProfile profile : this.profiles) {
                array.add((JsonElement)profile.toJson());
            }
            root.addProperty("trainingDurationSec", (Number)this.trainingDurationSec);
            root.add("profiles", (JsonElement)array);
            Files.writeString(path, (CharSequence)this.gson.toJson((JsonElement)root), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception e) {
            System.err.println("[AINew] Failed to save AI new rotations");
            e.printStackTrace();
        }
    }

    public List<AINewRotationProfile> getProfiles() {
        this.ensureLoaded();
        return List.copyOf(this.profiles);
    }

    public List<String> listProfileNames() {
        this.ensureLoaded();
        ArrayList<String> names = new ArrayList<String>(this.profiles.size());
        for (AINewRotationProfile profile : this.profiles) {
            names.add(profile.getName());
        }
        return names;
    }

    public AINewRotationProfile getProfile(String name) {
        this.ensureLoaded();
        if (name == null || name.isBlank()) {
            return null;
        }
        for (AINewRotationProfile profile : this.profiles) {
            if (!profile.getName().equalsIgnoreCase(name)) continue;
            return profile;
        }
        return null;
    }

    public AINewRotationProfile getProfileOrFallback(String name) {
        AINewRotationProfile profile = this.getProfile(name);
        return profile != null ? profile : AINewRotationProfile.fallback();
    }

    public void addOrReplace(AINewRotationProfile profile) {
        this.ensureLoaded();
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) {
            return;
        }
        this.profiles.removeIf(existing -> existing.getName().equalsIgnoreCase(profile.getName()));
        this.profiles.add(profile);
        this.profiles.sort(Comparator.comparing(AINewRotationProfile::getName, String.CASE_INSENSITIVE_ORDER));
        this.lastSavedProfileName = profile.getName();
        this.save();
        this.notifyListeners();
    }

    public boolean removeProfile(String name) {
        this.ensureLoaded();
        if (name == null || name.isBlank()) {
            return false;
        }
        boolean removed = this.profiles.removeIf(profile -> profile.getName().equalsIgnoreCase(name));
        if (!removed) {
            return false;
        }
        this.save();
        this.notifyListeners();
        return true;
    }

    public String[] getModeValues() {
        this.ensureLoaded();
        if (this.profiles.isEmpty()) {
            return new String[]{"None"};
        }
        String[] values = new String[this.profiles.size()];
        for (int i = 0; i < this.profiles.size(); ++i) {
            values[i] = this.profiles.get(i).getName();
        }
        return values;
    }

    public void addListener(Runnable listener) {
        if (listener == null) {
            return;
        }
        this.listeners.add(listener);
    }

    public String pollLastSavedProfileName() {
        String name = this.lastSavedProfileName;
        this.lastSavedProfileName = null;
        return name;
    }

    public AINewRotationProfile findFirstUsableProfile() {
        this.ensureLoaded();
        for (AINewRotationProfile profile : this.profiles) {
            if (profile == null || !profile.hasMotionClips()) continue;
            return profile;
        }
        return null;
    }

    private void notifyListeners() {
        for (Runnable listener : this.listeners) {
            try {
                listener.run();
            }
            catch (Exception e) {
                System.err.println("[AINew] Failed to notify AI new rotation listener");
                e.printStackTrace();
            }
        }
    }

    public boolean isTrainingActive() {
        return this.activeTraining != null && !this.activeTraining.isFinished();
    }

    public AINewCombatTrainingSession getActiveTraining() {
        return this.activeTraining;
    }

    public int getTrainingDurationSec() {
        this.ensureLoaded();
        return this.trainingDurationSec;
    }

    public long getTrainingDurationMs() {
        return (long)this.getTrainingDurationSec() * 1000L;
    }

    public boolean setTrainingDurationSec(int seconds) {
        this.ensureLoaded();
        int clamped = AINewRotationManager.clampTrainingDuration(seconds);
        if (clamped == this.trainingDurationSec) {
            return false;
        }
        this.trainingDurationSec = clamped;
        this.save();
        return true;
    }

    public static int clampTrainingDuration(int seconds) {
        return Math.max(15, Math.min(300, seconds));
    }

    public boolean startCombatTraining(String profileName) {
        if (profileName == null || profileName.isBlank() || this.isTrainingActive()) {
            return false;
        }
        this.activeTraining = new AINewCombatTrainingSession(profileName.trim(), this, this.getTrainingDurationMs());
        return true;
    }

    public boolean stopCombatTraining() {
        AINewCombatTrainingSession session = this.activeTraining;
        if (session == null || session.isFinished()) {
            return false;
        }
        session.stop();
        this.activeTraining = null;
        return true;
    }

    public void tickTraining(MinecraftClient client) {
        AINewCombatTrainingSession session = this.activeTraining;
        if (session == null) {
            return;
        }
        session.tick(client);
        if (session.isFinished()) {
            this.activeTraining = null;
        }
    }

    public void recordTrainingRealtime(MinecraftClient client) {
        AINewCombatTrainingSession session = this.activeTraining;
        if (session == null || session.isFinished()) {
            return;
        }
        session.recordRealtime(client);
    }

    public void onTrainingAttack(Entity target) {
        if (this.activeTraining != null && !this.activeTraining.isFinished()) {
            this.activeTraining.onAttack(target);
        }
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("cheap").resolve("ai_new_rotations.json");
    }
}

