package fun.ogi.command.impl;

import fun.ogi.Cheap;
import fun.ogi.command.Command;
import fun.ogi.util.chatutil.ChatUtil;
import fun.ogi.util.neuro.rotation.AIRotationManager;
import fun.ogi.util.neuro.rotation.AIRotationRecorder;

public class NeuroCommand extends Command {

    private static AIRotationRecorder recorder = null;

    public NeuroCommand() {
        super("neuro", "Neuro rotation pattern management (record/stop/save/train/load/list/clear/dir)", "nr", "ai");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> startRecording();
            case "stop" -> stopRecording();
            case "save" -> saveDataset(args);
            case "train" -> trainModel(args);
            case "load" -> loadModel(args);
            case "list" -> AIRotationManager.listFiles();
            case "clear" -> clearSamples();
            case "dir" -> AIRotationManager.openDirectory();
            default -> {
                ChatUtil.sendMSG("§cНеизвестная подкоманда: §f" + args[0]);
                printHelp();
            }
        }
    }

    private void startRecording() {
        if (AIRotationRecorder.isRecording()) {
            ChatUtil.sendMSG("§cЗапись уже идёт!");
            return;
        }

        if (recorder == null) {
            recorder = new AIRotationRecorder();
            Cheap.getInstance().getEventBus().register(recorder);
        }

        AIRotationRecorder.startRecording();
        ChatUtil.sendMSG("§aЗапись начата!");
        ChatUtil.sendMSG("§7Играйте с аурой, пока не наберёте нужные сэмплы");
        ChatUtil.sendMSG("§7Остановите через §f.neuro stop");
    }

    private void stopRecording() {
        if (!AIRotationRecorder.isRecording()) {
            ChatUtil.sendMSG("§cЗапись не идёт!");
            return;
        }

        int samples = AIRotationRecorder.stopRecording();
        ChatUtil.sendMSG("§aЗапись остановлена!");
        ChatUtil.sendMSG("§7Собрано сэмплов: §f" + samples);
        ChatUtil.sendMSG("§7Сохраните через §f.neuro save <name>");
    }

    private void saveDataset(String[] args) {
        if (args.length < 2) {
            usage("save <name>");
            return;
        }
        AIRotationManager.saveDataset(args[1]);
    }

    private void trainModel(String[] args) {
        if (args.length < 3) {
            usage("train <dataset> <model>");
            return;
        }

        String datasetName = args[1];
        String modelName = args[2];

        ChatUtil.sendMSG("§7Запускаю обучение в фоне...");

        new Thread(() -> AIRotationManager.trainModel(datasetName, modelName)).start();
    }

    private void loadModel(String[] args) {
        if (args.length < 2) {
            usage("load <model>");
            return;
        }
        AIRotationManager.loadModel(args[1]);
    }

    private void clearSamples() {
        if (AIRotationRecorder.isRecording()) {
            AIRotationRecorder.stopRecording();
        }
        AIRotationRecorder.clearSamples();
        ChatUtil.sendMSG("§aСэмплы очищены");
    }

    private void printHelp() {
        ChatUtil.sendMSG("§e§l=== Neuro Rotation Commands ===");
        ChatUtil.sendMSG("§f.neuro start §7- начать запись паттерна");
        ChatUtil.sendMSG("§f.neuro stop §7- остановить запись");
        ChatUtil.sendMSG("§f.neuro save <name> §7- сохранить датасет");
        ChatUtil.sendMSG("§f.neuro train <dataset> <model> §7- обучить модель");
        ChatUtil.sendMSG("§f.neuro load <model> §7- загрузить модель");
        ChatUtil.sendMSG("§f.neuro list §7- список файлов");
        ChatUtil.sendMSG("§f.neuro clear §7- очистить сэмплы");
        ChatUtil.sendMSG("§f.neuro dir §7- открыть папку");
    }
}

