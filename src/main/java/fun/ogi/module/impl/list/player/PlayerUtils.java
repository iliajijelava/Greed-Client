package fun.ogi.module.impl.list.player;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.AttackEvent;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.ModeSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.time.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;

@ModuleInformation(moduleName = "Player Utils", moduleDesc = "Утилиты для игрока", moduleCategory = ModuleCategory.PLAYER)
public class PlayerUtils extends Module {
    private final BooleanSetting antiAfk = new BooleanSetting("Anti AFK", this, false);
    private final BooleanSetting autoRespawn = new BooleanSetting("Auto Respawn", this, false);
    private final BooleanSetting autoFish = new BooleanSetting("Auto Fish", this, false);
    private final BooleanSetting fastLadder = new BooleanSetting("Fast Ladder", this, false);
    private final BooleanSetting noFriendDamage = new BooleanSetting("No Friend Damage", this, false);

    private final ModeSetting antiAfkMode = new ModeSetting("Anti AFK Mode", this, "Chat", "Chat", "Jump", "Swing")
            .visible(() -> antiAfk.getValue());
    private final SliderSetting delay = new SliderSetting("Anti AFK Delay", this, 50, 5, 60, 5)
            .visible(() -> antiAfk.getValue());

    private final Timer timerAFK = new Timer();
    private final Timer fishTimer = new Timer();
    private boolean hookFlag;
    private boolean thrown;
    private boolean activeAFK;

    public PlayerUtils() {
        addSettings(antiAfk, autoRespawn, autoFish, fastLadder, noFriendDamage, antiAfkMode, delay);
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        if (noFriendDamage.getValue()
                && event.getEntity() instanceof PlayerEntity
                && Cheap.getInstance().getFriendManager().contains(event.getEntity().getName().getString())) {
            event.cancelEvent();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (antiAfk.getValue()) {
            if (timerAFK.finished(10000L)) {
                activeAFK = true;
            }

            if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
                activeAFK = false;
                timerAFK.reset();
            }

            if (activeAFK && mc.player.age % delay.getIntValue() == 5) {
                if (antiAfkMode.is("Chat")) {
                    mc.player.networkHandler.sendChatMessage("! Всем привет " + Math.random() + " !");
                } else if (antiAfkMode.is("Jump") && mc.player.isOnGround()) {
                    mc.player.jump();
                } else if (antiAfkMode.is("Swing")) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }

        if (autoRespawn.getValue() && mc.currentScreen instanceof DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        }

        if (autoFish.getValue() && mc.player.getMainHandStack().getItem() instanceof FishingRodItem) {
            if (mc.player.fishHook != null) {
                thrown = true;
                fishTimer.reset();
                if (!hookFlag && mc.player.fishHook.getDataTracker().get(FishingBobberEntity.CAUGHT_FISH)) {
                    throwRod();
                    hookFlag = true;
                    fishTimer.reset();
                }
            } else if (hookFlag && fishTimer.finished(600L)) {
                throwRod();
                hookFlag = false;
                thrown = false;
                fishTimer.reset();
            } else if (!hookFlag && thrown && fishTimer.finished(3000L)) {
                throwRod();
                thrown = false;
                fishTimer.reset();
            }
        }

        if (fastLadder.getValue() && mc.world.getBlockState(mc.player.getBlockPos()).isOf(Blocks.LADDER)) {
            mc.player.setVelocity(mc.player.getVelocity().multiply(1.0, 1.43, 1.0));
        }
    }

    private void throwRod() {
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public void onDisable() {
        hookFlag = false;
        thrown = false;
        activeAFK = false;
    }
}

