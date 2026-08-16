package fun.ogi.module.impl.list.movement;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.PacketEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.ModeSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "InventoryMove", moduleDesc = "Move while in inventory", moduleCategory = ModuleCategory.MOVEMENT)
public class InventoryMove extends Module {
    public static InventoryMove INSTANCE = new InventoryMove();
    public boolean swapBypass;
    private final ModeSetting mode = new ModeSetting("Mode", this, "Normal", "Normal", "Legit");

    private final List<PacketEvent> heldPackets = new ArrayList<>();
    private boolean flushing = false;

    public InventoryMove() {
        addSetting(mode);
    }

    @Override
    public void onDisable() {
        heldPackets.clear();
        flushing = false;
    }

    @Subscribe
    public void onPacket(PacketEvent e) {
        if (e.isSend()) {
            if (e.getPacket() instanceof ClickSlotC2SPacket) {
                if (!flushing && !swapBypass && isMoving()) {
                    heldPackets.add(e);
                    e.cancelEvent();
                } else {
                    flushHeldPackets();
                }
            } else if (e.getPacket() instanceof CloseHandledScreenC2SPacket) {
                flushHeldPackets();
            }
        } else {
            if (e.getPacket() instanceof CloseScreenS2CPacket) {
                heldPackets.clear();
            }
        }
    }

    @Subscribe
    public void onTick(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen instanceof ChatScreen) return;

        boolean hasScreen = mc.currentScreen != null;

        if (hasScreen) {
            forceMovementKeys();
        }

        if (mode.getValue().equals("Legit")) {
            if (hasScreen && !isMoving()) {
                flushHeldPackets();
            }
        }
    }

    private void forceMovementKeys() {
        long handle = mc.getWindow().getHandle();
        boolean forward = InputUtil.isKeyPressed(handle, mc.options.forwardKey.getDefaultKey().getCode());
        boolean back = InputUtil.isKeyPressed(handle, mc.options.backKey.getDefaultKey().getCode());
        boolean left = InputUtil.isKeyPressed(handle, mc.options.leftKey.getDefaultKey().getCode());
        boolean right = InputUtil.isKeyPressed(handle, mc.options.rightKey.getDefaultKey().getCode());
        boolean jump = InputUtil.isKeyPressed(handle, mc.options.jumpKey.getDefaultKey().getCode());

        mc.options.forwardKey.setPressed(forward);
        mc.options.backKey.setPressed(back);
        mc.options.leftKey.setPressed(left);
        mc.options.rightKey.setPressed(right);
        mc.options.jumpKey.setPressed(jump);
    }

    private boolean isMoving() {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    private void flushHeldPackets() {
        if (heldPackets.isEmpty() || flushing) return;
        if (mc.getNetworkHandler() == null) {
            heldPackets.clear();
            return;
        }

        flushing = true;
        try {
            List<Packet<?>> packets = new ArrayList<>();
            for (PacketEvent held : heldPackets) {
                packets.add(held.getPacket());
            }
            heldPackets.clear();
            for (Packet<?> packet : packets) {
                mc.getNetworkHandler().sendPacket(packet);
            }
        } finally {
            flushing = false;
        }
    }
}
