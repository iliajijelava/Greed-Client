package fun.ogi.util.chatutil;

import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;

import static fun.ogi.util.MinecraftUtil.mc;


public class ChatUtil {

    public static void sendMSG(String msg){
        if (mc.player == null || mc.world == null) return;

        mc.player.sendMessage(
                Text.literal("")
                        .append(prefix())
                        .append(Text.literal(msg)),
                false
        );
    }

    private static MutableText prefix() {
        return gradient("Greed", 0x00FFAA, 0xAA00FF)
                .append(Text.literal(" §f» "));
    }

    private static MutableText gradient(String text, int startColor, int endColor) {
        MutableText result = Text.literal("");

        for (int i = 0; i < text.length(); i++) {
            float t = (float) i / (text.length() - 1);

            int color = lerpColor(startColor, endColor, t);

            result.append(Text.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }

        return result;
    }

    private static int lerpColor(int start, int end, float t) {
        int r1 = (start >> 16) & 0xFF;
        int g1 = (start >> 8) & 0xFF;
        int b1 = start & 0xFF;

        int r2 = (end >> 16) & 0xFF;
        int g2 = (end >> 8) & 0xFF;
        int b2 = end & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }
}