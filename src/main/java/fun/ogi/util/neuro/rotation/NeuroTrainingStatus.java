package fun.ogi.util.neuro.rotation;

public class NeuroTrainingStatus {

    public enum State {
        IDLE, TRAINING, DONE, ERROR
    }

    private static volatile State state = State.IDLE;
    private static volatile String datasetName = "";
    private static volatile String modelName = "";
    private static volatile int samples = 0;
    private static volatile int epoch = 0;
    private static volatile int totalEpochs = 1;
    private static volatile float loss = -1f;
    private static volatile String error = "";
    private static volatile long completedAt = 0;

    private static final long COMPLETED_HIDE_MS = 5000;

    public static void setTraining(String datasetName, String modelName, int samples, int totalEpochs) {
        NeuroTrainingStatus.datasetName = datasetName;
        NeuroTrainingStatus.modelName = modelName;
        NeuroTrainingStatus.samples = samples;
        NeuroTrainingStatus.epoch = 0;
        NeuroTrainingStatus.totalEpochs = Math.max(1, totalEpochs);
        NeuroTrainingStatus.loss = -1f;
        NeuroTrainingStatus.error = "";
        NeuroTrainingStatus.completedAt = 0;
        state = State.TRAINING;
    }

    public static void updateProgress(int epoch, float loss) {
        NeuroTrainingStatus.epoch = epoch;
        NeuroTrainingStatus.loss = loss;
    }

    public static void setDone(float loss) {
        NeuroTrainingStatus.loss = loss;
        NeuroTrainingStatus.error = "";
        NeuroTrainingStatus.completedAt = System.currentTimeMillis();
        state = State.DONE;
    }

    public static void setError(String error) {
        NeuroTrainingStatus.error = error != null ? error : "";
        NeuroTrainingStatus.completedAt = System.currentTimeMillis();
        state = State.ERROR;
    }

    public static boolean shouldHide() {
        return state != State.TRAINING
                && completedAt > 0
                && System.currentTimeMillis() - completedAt > COMPLETED_HIDE_MS;
    }

    public static void reset() {
        datasetName = "";
        modelName = "";
        samples = 0;
        epoch = 0;
        totalEpochs = 1;
        loss = -1f;
        error = "";
        completedAt = 0;
        state = State.IDLE;
    }

    public static boolean isTraining() {
        return state == State.TRAINING;
    }

    public static State getState() {
        return state;
    }

    public static String getDatasetName() {
        return datasetName;
    }

    public static String getModelName() {
        return modelName;
    }

    public static int getSamples() {
        return samples;
    }

    public static int getEpoch() {
        return epoch;
    }

    public static int getTotalEpochs() {
        return totalEpochs;
    }

    public static float getLoss() {
        return loss;
    }

    public static String getError() {
        return error;
    }
}

