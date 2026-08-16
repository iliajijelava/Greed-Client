package fun.ogi.module.impl.list.misc;

import com.google.common.eventbus.Subscribe;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.*;
import fun.ogi.util.NotificationManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Server Helper", moduleDesc = "Helper for servers", moduleCategory = ModuleCategory.MISC)
public class ServerHelper extends Module {

    public static ServerHelper INSTANCE = new ServerHelper();

    private final ModeSetting mode = new ModeSetting("Mode", this, "HolyWorld", "HolyWorld", "ReallyWorld", "LonyGrief", "VonTam");

    private final KeySetting stickHW = new KeySetting("Explosion Stick", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));
    private final KeySetting gulHW = new KeySetting("Ghoul", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));
    private final KeySetting stunHW = new KeySetting("Stun", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));
    private final KeySetting trapExplosionHW = new KeySetting("Trap Explosion", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));
    private final KeySetting snowHW = new KeySetting("Snow", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));
    private final KeySetting trapHW = new KeySetting("Trap", this, -1).visible(() -> mode.getValueAsString().equals("HolyWorld"));

    private final KeySetting antiFlyRW = new KeySetting("Anti Fly", this, -1).visible(() -> mode.getValueAsString().equals("ReallyWorld"));
    private final KeySetting trapRW = new KeySetting("Trap", this, -1).visible(() -> mode.getValueAsString().equals("ReallyWorld"));

    private final KeySetting uniqueTrapLG = new KeySetting("Unique Trap", this, -1).visible(() -> mode.getValueAsString().equals("LonyGrief"));
    private final KeySetting defLivaLG = new KeySetting("Def Liva", this, -1).visible(() -> mode.getValueAsString().equals("LonyGrief"));
    private final KeySetting platformLivaLG = new KeySetting("Platform Liva", this, -1).visible(() -> mode.getValueAsString().equals("LonyGrief"));

    private final KeySetting disorientationVT = new KeySetting("Disorientation", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));
    private final KeySetting trapVT = new KeySetting("Trap", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));
    private final KeySetting plastVT = new KeySetting("Plast", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));
    private final KeySetting dustVT = new KeySetting("Dust", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));
    private final KeySetting snowFreezeVT = new KeySetting("Snow Freeze", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));
    private final KeySetting godAuraVT = new KeySetting("God Aura", this, -1).visible(() -> mode.getValueAsString().equals("VonTam"));

    private final BooleanSetting showSwap = new BooleanSetting("Show notifications", this, true);
    private final SliderSetting delay = new SliderSetting("Delay (ticks)", this, 2, 1, 20, 1);

    private int targetSlot = -1;
    private int oldSlot = -1;
    private int tickCounter = 0;
    private boolean isPerforming = false;
    private boolean isSwapped = false;
    private String currentActionName = "";

    public ServerHelper() {
        addSettings(
                mode,
                stickHW, gulHW, stunHW, trapExplosionHW, snowHW, trapHW,
                antiFlyRW, trapRW,
                uniqueTrapLG, defLivaLG, platformLivaLG,
                disorientationVT, trapVT, plastVT, dustVT, snowFreezeVT, godAuraVT,
                showSwap, delay
        );
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (isPerforming && mc.player != null) {
            finishAction();
        }
        reset();
        super.onDisable();
    }

    private void reset() {
        targetSlot = -1;
        oldSlot = -1;
        tickCounter = 0;
        isPerforming = false;
        isSwapped = false;
        currentActionName = "";
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) {
            reset();
            return;
        }

        if (isPerforming) {
            tickCounter++;
            if (tickCounter >= (int) delay.getValue()) {
                if (mc.currentScreen == null) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                }
                finishAction();
            }
            return;
        }

        if (mc.currentScreen != null) return;

        String currentMode = mode.getValueAsString();

        if (currentMode.equals("HolyWorld")) {
            if (stickHW.justPressed()) startAction(Items.FIRE_CHARGE, "Explosion Stick");
            else if (gulHW.justPressed()) startAction(Items.FIREWORK_STAR, "Ghoul");
            else if (stunHW.justPressed()) startAction(Items.NETHER_STAR, "Stun");
            else if (trapExplosionHW.justPressed()) startAction(Items.PRISMARINE_SHARD, "Trap Explosion");
            else if (snowHW.justPressed()) startAction(Items.SNOWBALL, "Snow");
            else if (trapHW.justPressed()) startAction(Items.POPPED_CHORUS_FRUIT, "Trap");
        } else if (currentMode.equals("ReallyWorld")) {
            if (antiFlyRW.justPressed()) startAction(Items.FIREWORK_STAR, "Anti Fly");
            else if (trapRW.justPressed()) startAction(Items.HEART_OF_THE_SEA, "Trap");
        } else if (currentMode.equals("LonyGrief")) {
            if (uniqueTrapLG.justPressed()) startAction(Items.CRYING_OBSIDIAN, "Unique Trap");
            else if (defLivaLG.justPressed()) startAction(Items.MAGMA_CREAM, "Def Liva");
            else if (platformLivaLG.justPressed()) startAction(Items.CLAY_BALL, "Platform Liva");
        } else if (currentMode.equals("VonTam")) {
            if (disorientationVT.justPressed()) startAction(Items.ENDER_EYE, "Disorientation");
            else if (trapVT.justPressed()) startAction(Items.NETHERITE_SCRAP, "Trap");
            else if (plastVT.justPressed()) startAction(Items.DRIED_KELP, "Plast");
            else if (dustVT.justPressed()) startAction(Items.SUGAR, "Dust");
            else if (snowFreezeVT.justPressed()) startAction(Items.SNOWBALL, "Snow Freeze");
            else if (godAuraVT.justPressed()) startAction(Items.PHANTOM_MEMBRANE, "God Aura");
        }
    }

    private void finishAction() {
        if (isSwapped) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, targetSlot, oldSlot, SlotActionType.SWAP, mc.player);
        } else {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }
        isPerforming = false;
        if (showSwap.getValue()) {
            NotificationManager.post("Server Helper " + currentActionName + ": done!");
        }
    }

    private void startAction(Item item, String name) {
        int slot = findItem(item);
        if (slot == -1) {
            if (showSwap.getValue()) {
                NotificationManager.post("Server Helper " + name + ": Item not found!");
            }
            return;
        }

        this.oldSlot = mc.player.getInventory().selectedSlot;
        this.targetSlot = slot;
        this.tickCounter = 0;
        this.isPerforming = true;
        this.currentActionName = name;

        if (targetSlot < 9) {
            mc.player.getInventory().selectedSlot = targetSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
            this.isSwapped = false;
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, targetSlot, oldSlot, SlotActionType.SWAP, mc.player);
            this.isSwapped = true;
        }
    }

    private int findItem(Item item) {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 35; i >= 0; i--) {
            ItemStack stack = inv.getStack(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}

