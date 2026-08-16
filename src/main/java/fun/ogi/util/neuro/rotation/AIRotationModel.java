package fun.ogi.util.neuro.rotation;

import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.nn.norm.BatchNorm;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.initializer.XavierInitializer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Adam;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.TranslateException;
import fun.ogi.util.chatutil.ChatUtil;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public class AIRotationModel implements Closeable {

    private static final int NUM_EPOCH = 1000;
    private static final int BATCH_SIZE = 32;
    private static final int INPUT_SIZE = 4;
    private static final int OUTPUT_SIZE = 2;

    private static final float YAW_NORM = 180f;
    private static final float PITCH_NORM = 90f;

    private final Model model;
    private final Predictor<float[], float[]> predictor;
    private final String name;

    public AIRotationModel(String name) {
        this.name = name;
        this.model = Model.newInstance(name);
        this.model.setBlock(createMlpBlock());
        this.predictor = model.newPredictor(new FloatArrayTranslator());
    }

    public float[] predict(float[] input) throws TranslateException {
        return denormalizeOutput(predictor.predict(normalizeInput(input)));
    }

    public float train(float[][] features, float[][] labels) throws ModelException, IOException, TranslateException {
        if (features.length != labels.length || features.length == 0) {
            throw new IllegalArgumentException("Features and labels must have the same size and be non-empty");
        }

        ChatUtil.sendMSG("§aНачинаю обучение модели §e" + name + "§a...");
        ChatUtil.sendMSG("§7Сэмплов: §f" + features.length + " §7| Эпох: §f" + NUM_EPOCH);

        float[][] normFeatures = new float[features.length][];
        float[][] normLabels = new float[labels.length][];
        for (int i = 0; i < features.length; i++) {
            normFeatures[i] = normalizeInput(features[i]);
            normLabels[i] = normalizeLabel(labels[i]);
        }

        TrainingConfig trainingConfig = new DefaultTrainingConfig(Loss.l2Loss())
                .optInitializer(new XavierInitializer(), "weight")
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(0.001f)).build())
                .addTrainingListeners(TrainingListener.Defaults.logging("train"))
                .addTrainingListeners(new ProgressListener());

        float finalLoss;
        try (Trainer trainer = model.newTrainer(trainingConfig);
             NDManager manager = NDManager.newBaseManager()) {

            ArrayDataset trainingSet = new ArrayDataset.Builder()
                    .setData(manager.create(normFeatures))
                    .optLabels(manager.create(normLabels))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            trainer.initialize(new Shape(BATCH_SIZE, INPUT_SIZE));
            EasyTrain.fit(trainer, NUM_EPOCH, trainingSet, null);

            finalLoss = trainer.getTrainingResult().getTrainLoss() != null
                    ? trainer.getTrainingResult().getTrainLoss() : -1f;

            ChatUtil.sendMSG("§aОбучение завершено! Итоговый loss: §f" + String.format("%.5f", finalLoss));
        }
        return finalLoss;
    }

    public static float[] normalizeInput(float[] input) {
        return new float[]{
                input[0] / YAW_NORM,
                input[1] / PITCH_NORM,
                input[2] / YAW_NORM,
                input[3] / PITCH_NORM
        };
    }

    public static float[] normalizeLabel(float[] output) {
        return new float[]{
                output[0] / YAW_NORM,
                output[1] / PITCH_NORM
        };
    }

    public static float[] denormalizeOutput(float[] output) {
        return new float[]{
                output[0] * YAW_NORM,
                output[1] * PITCH_NORM
        };
    }

    public void load(Path path) throws IOException, ModelException {
        model.load(path, "model");
        ChatUtil.sendMSG("§aМодель §e" + name + " §aзагружена");
    }

    public void save(Path path) throws IOException {
        model.save(path, "model");
        ChatUtil.sendMSG("§aМодель §e" + name + " §aсохранена");
    }

    @Override
    public void close() {
        predictor.close();
        model.close();
    }

    private static SequentialBlock createMlpBlock() {
        return new SequentialBlock()
                .add(Linear.builder().setUnits(128).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(64).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(32).build())
                .add(Blocks.batchFlattenBlock())
                .add(BatchNorm.builder().build())
                .add(Activation.reluBlock())

                .add(Linear.builder().setUnits(OUTPUT_SIZE).build());
    }

    private static class ProgressListener implements TrainingListener {

        private int epochCount = 0;

        @Override
        public void onEpoch(Trainer trainer) {
            epochCount++;
            Float loss = trainer.getTrainingResult().getTrainLoss();
            NeuroTrainingStatus.updateProgress(epochCount, loss != null ? loss : -1f);
        }

        @Override
        public void onTrainingBatch(Trainer trainer, BatchData batchData) {
        }

        @Override
        public void onValidationBatch(Trainer trainer, BatchData batchData) {
        }

        @Override
        public void onTrainingBegin(Trainer trainer) {
        }

        @Override
        public void onTrainingEnd(Trainer trainer) {
        }
    }
}

