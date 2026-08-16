package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.ModuleToggleEvent;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;

@ModuleInformation(moduleName = "Client Sounds", moduleDesc = "Plays client sounds", moduleCategory = ModuleCategory.MISC)
public class ClientSounds extends Module {

    public static ClientSounds INSTANCE = new ClientSounds();

    public final BooleanSetting playClickGuiSound = new BooleanSetting("Click GUI Sound", this, true);
    public final ModeSetting stateSound = new ModeSetting("State Sound", this, "First",
            "None", "First", "Second", "Third", "Fourth", "Fifth", "Sixth");
    public final SliderSetting volume = new SliderSetting("Volume", this, 50.0f, 1.0f, 100.0f, 0.5f);

    public ClientSounds() {
        addSettings(playClickGuiSound, stateSound, volume);
    }

    public void playSound(String soundName) {
        String path = switch (soundName) {
            case "OpenGUI" -> "opengui";
            case "CloseGUI" -> "closegui";
            default -> soundName.toLowerCase();
        };
        float vol = volume.getFloatValue() / 100.0f;
        String resourcePath = "/assets/cheap/sounds/" + path + ".wav";
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                is = ClientSounds.class.getResourceAsStream(resourcePath);
            }
            if (is == null) return;
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = vol > 0.0f ? Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), 20f * (float) Math.log10(vol))) : gain.getMinimum();
                gain.setValue(dB);
            }
            clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
            clip.start();
        } catch (Exception ignored) {
        }
    }

    @Subscribe
    public void onModuleToggle(ModuleToggleEvent event) {
        String mode = stateSound.getValue();
        if (mode.equals("None")) return;
        if (event.getModule() == this) return;
        playSound(mode);
    }

    public void playOpenGui() {
        if (!isEnabled() || !playClickGuiSound.getValue()) return;
        playSound("OpenGUI");
    }

    public void playCloseGui() {
        if (!isEnabled() || !playClickGuiSound.getValue()) return;
        playSound("CloseGUI");
    }
}

