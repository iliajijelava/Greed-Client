package fun.ogi.util.neuro.rotation;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import static fun.ogi.util.MinecraftUtil.mc;

public class NeuroOverlay {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFC94D;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int GRAY = 0xFFAAAAAA;

    @Subscribe
    public void onHud(EventHud event) {
        if (mc.player == null || mc.world == null) return;

        NeuroTrainingStatus.State state = NeuroTrainingStatus.getState();
        if (state == NeuroTrainingStatus.State.IDLE) return;

        if (NeuroTrainingStatus.shouldHide()) {
            NeuroTrainingStatus.reset();
            return;
        }

        DrawContext context = event.getDrawContext();
        MutableText text = buildText(state);
        if (text == null) return;

        TextRenderer renderer = mc.textRenderer;
        int width = renderer.getWidth(text);
        int x = (mc.getWindow().getScaledWidth() - width) / 2;
        int y = mc.getWindow().getScaledHeight() - 62;

        int color = switch (state) {
            case TRAINING -> YELLOW;
            case DONE -> GREEN;
            case ERROR -> RED;
            default -> WHITE;
        };

        context.drawText(renderer, text, x, y, color, true);
    }

    private MutableText buildText(NeuroTrainingStatus.State state) {
        return switch (state) {
            case TRAINING -> Text.literal("")
                    .append(part("Neuro: обучение ", WHITE))
                    .append(part("'" + NeuroTrainingStatus.getModelName() + "'", YELLOW))
                    .append(part("  |  датасет: ", GRAY))
                    .append(part(NeuroTrainingStatus.getDatasetName(), WHITE))
                    .append(part("  |  ", GRAY))
                    .append(part(NeuroTrainingStatus.getEpoch() + "/" + NeuroTrainingStatus.getTotalEpochs(), YELLOW))
                    .append(part(" эпох  |  loss: ", GRAY))
                    .append(part(formatLoss(), WHITE));
            case DONE -> Text.literal("")
                    .append(part("Neuro: модель ", WHITE))
                    .append(part("'" + NeuroTrainingStatus.getModelName() + "'", YELLOW))
                    .append(part(" обучена  |  loss: ", GRAY))
                    .append(part(formatLoss(), WHITE));
            case ERROR -> Text.literal("")
                    .append(part("Neuro: ошибка обучения '", WHITE))
                    .append(part(NeuroTrainingStatus.getModelName(), YELLOW))
                    .append(part("': ", WHITE))
                    .append(part(NeuroTrainingStatus.getError(), RED));
            default -> null;
        };
    }

    private MutableText part(String text, int color) {
        return Text.literal(text).setStyle(Style.EMPTY.withColor(color));
    }

    private String formatLoss() {
        float loss = NeuroTrainingStatus.getLoss();
        return loss < 0 ? "--" : String.format("%.5f", loss);
    }
}

