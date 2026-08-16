package fun.ogi.util.neuro.rotation;

import ai.djl.ModelException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fun.ogi.util.chatutil.ChatUtil;


import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AIRotationManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path AI_DIR = Paths.get("greed", "ai");
    private static final Path DATASETS_DIR = AI_DIR.resolve("datasets");
    private static final Path MODELS_DIR = AI_DIR.resolve("models");

    private static AIRotationModel currentModel = null;
    
    static {
        try {
            Files.createDirectories(DATASETS_DIR);
            Files.createDirectories(MODELS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveDataset(String name) {
        List<TrainingSample> samples = AIRotationRecorder.getSamples();
        if (samples.isEmpty()) {
            ChatUtil.sendMSG("§cНет данных для сохранения! Используйте .ai start для начала записи");
            return;
        }

        try {
            Path datasetPath = DATASETS_DIR.resolve(name + ".json");
            try (FileWriter writer = new FileWriter(datasetPath.toFile())) {
                GSON.toJson(samples, writer);
            }
            ChatUtil.sendMSG("§aДатасет §e" + name + " §aсохранен (§f" + samples.size() + " §aсэмплов)");
            ChatUtil.sendMSG("§7Путь: §f" + datasetPath.toAbsolutePath());
        } catch (IOException e) {
            ChatUtil.sendMSG("§cОшибка сохранения датасета: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void trainModel(String datasetName, String modelName) {
        if (NeuroTrainingStatus.isTraining()) {
            ChatUtil.sendMSG("§cОбучение уже идёт! Дождитесь завершения.");
            return;
        }

        try {
            
            Path datasetPath = DATASETS_DIR.resolve(datasetName + ".json");
            if (!Files.exists(datasetPath)) {
                ChatUtil.sendMSG("§cДатасет §e" + datasetName + " §cне найден!");
                return;
            }

            Type listType = new TypeToken<List<TrainingSample>>(){}.getType();
            List<TrainingSample> samples;
            
            try (FileReader reader = new FileReader(datasetPath.toFile())) {
                samples = GSON.fromJson(reader, listType);
            }

            if (samples == null || samples.isEmpty()) {
                ChatUtil.sendMSG("§cДатасет пуст!");
                return;
            }

            
            float[][] features = new float[samples.size()][];
            float[][] labels = new float[samples.size()][];
            
            for (int i = 0; i < samples.size(); i++) {
                features[i] = samples.get(i).getInput();
                labels[i] = samples.get(i).getOutput();
            }

            NeuroTrainingStatus.setTraining(datasetName, modelName, samples.size(), 1000);

            
            AIRotationModel model = new AIRotationModel(modelName);
            float finalLoss;
            try {
                finalLoss = model.train(features, labels);
            } catch (Exception e) {
                model.close();
                NeuroTrainingStatus.setError(e.getMessage());
                ChatUtil.sendMSG("§cОшибка обучения модели: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            
            Path modelPath = MODELS_DIR.resolve(modelName);
            model.save(modelPath);
            
            model.close();

            NeuroTrainingStatus.setDone(finalLoss);
            
            ChatUtil.sendMSG("§aМодель §e" + modelName + " §aуспешно обучена и сохранена!");
            
        } catch (IOException e) {
            ChatUtil.sendMSG("§cОшибка обучения модели: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadModel(String modelName) {
        try {
            Path modelPath = MODELS_DIR.resolve(modelName);
            if (!Files.exists(modelPath)) {
                ChatUtil.sendMSG("§cМодель §e" + modelName + " §cне найдена!");
                System.out.println("MODEL PATH NOT FOUND: " + modelPath.toAbsolutePath());
                return;
            }

            if (currentModel != null) {
                currentModel.close();
            }

            System.out.println("Loading model from: " + modelPath.toAbsolutePath());
            currentModel = new AIRotationModel(modelName);
            currentModel.load(modelPath);
            
            ChatUtil.sendMSG("§aМодель §e" + modelName + " §aактивна!");
            System.out.println("MODEL LOADED SUCCESSFULLY: " + modelName);
            
        } catch (IOException | ModelException e) {
            ChatUtil.sendMSG("§cОшибка загрузки модели: " + e.getMessage());
            System.out.println("MODEL LOAD ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static float[] predict(float[] input) {
        if (currentModel == null) {
            System.out.println("AI MODEL IS NULL! Load a model first.");
            return new float[]{0, 0};
        }

        try {
            System.out.println("AI Input: [" + input[0] + ", " + input[1] + ", " + input[2] + ", " + input[3] + "]");
            float[] result = currentModel.predict(input);
            System.out.println("AI Output: [" + result[0] + ", " + result[1] + "]");
            return result;
        } catch (Exception e) {
            System.out.println("AI PREDICTION ERROR: " + e.getMessage());
            e.printStackTrace();
            return new float[]{0, 0};
        }
    }

    public static void listFiles() {
        ChatUtil.sendMSG("§e§l=== AI Rotation Files ===");
        
        
        File[] datasets = DATASETS_DIR.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (datasets != null && datasets.length > 0) {
            ChatUtil.sendMSG("§aДатасеты:");
            for (File dataset : datasets) {
                String name = dataset.getName().replace(".json", "");
                ChatUtil.sendMSG("  §7- §f" + name);
            }
        } else {
            ChatUtil.sendMSG("§7Датасеты: §cнет");
        }

        
        File[] models = MODELS_DIR.toFile().listFiles(File::isDirectory);
        if (models != null && models.length > 0) {
            ChatUtil.sendMSG("§aМодели:");
            for (File model : models) {
                String name = model.getName();
                String status = currentModel != null && currentModel.toString().contains(name) ? " §a(активна)" : "";
                ChatUtil.sendMSG("  §7- §f" + name + status);
            }
        } else {
            ChatUtil.sendMSG("§7Модели: §cнет");
        }
    }

    public static void openDirectory() {
        try {
            Desktop.getDesktop().open(AI_DIR.toFile());
            ChatUtil.sendMSG("§aПапка AI открыта");
        } catch (IOException e) {
            ChatUtil.sendMSG("§cОшибка открытия папки: " + e.getMessage());
            ChatUtil.sendMSG("§7Путь: §f" + AI_DIR.toAbsolutePath());
        }
    }
}

