package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.player.EventMoveInput;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.impl.list.movement.Sprint;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.NumberSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.combat.IdealHitUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;


@ModuleInformation(
        moduleName = "TriggerBot",
        moduleCategory = ModuleCategory.COMBAT,
        moduleDesc = "Auto attacks entities under crosshair"
)
public class TriggerBot extends Module {


    private final NumberSetting range =
            new NumberSetting("Range", this, 3.0, 0.0, 6.0, 0.05);


    private final BooleanSetting smartCrits =
            new BooleanSetting("Smart Crits", this, true);

    private final BooleanSetting sprintReset =
            new BooleanSetting("Sprint Reset", this, true);

    private final BooleanSetting throughWalls =
            new BooleanSetting("Through Walls", this, false);

    private final BooleanSetting aimCheck =
            new BooleanSetting("Aim Check", this, true);

    private final BooleanSetting shieldBypass =
            new BooleanSetting("Shield Bypass", this, false);

    private final BooleanSetting shieldBreak =
            new BooleanSetting("Shield Break", this, true);

    private final BooleanSetting attackWhileEating =
            new BooleanSetting("Attack While Eating", this, false);

    private final SliderSetting hitDelay =
            new SliderSetting("CD Between Hits", this, 0, 0, 1000, 50);


    private final BooleanSetting players =
            new BooleanSetting("Players", this, true);

    private final BooleanSetting invisible =
            new BooleanSetting("Invisible", this, true);

    private final BooleanSetting passive =
            new BooleanSetting("Passive", this, false);

    private final BooleanSetting hostile =
            new BooleanSetting("Hostile", this, true);


    private final IdealHitUtils idealHitUtils =
            new IdealHitUtils();


    private LivingEntity target;


    private boolean needSprintReset;
    private boolean sprintResetDone;

    private int sprintResetTicks;
    private int attackCooldown;
    private long lastAttackAt = -1L;


    public TriggerBot() {

        addSettings(
                range,
                smartCrits,
                sprintReset,
                throughWalls,
                aimCheck,
                shieldBypass,
                shieldBreak,
                attackWhileEating,
                hitDelay,
                players,
                invisible,
                passive,
                hostile
        );
    }


    @Subscribe
    public void onMoveInput(EventMoveInput event) {

        if (needSprintReset) {

            event.setForward(0);
            event.setStrafe(0);

            needSprintReset = false;
            sprintResetDone = true;
            sprintResetTicks = 0;
        }
    }


    @Subscribe
    public void onUpdate(EventUpdate event) {


        if (mc.player == null || mc.world == null)
            return;


        if (attackCooldown > 0)
            attackCooldown--;


        if (sprintResetDone)
            sprintResetTicks++;


        target = getTargetUnderCrosshair();


        if (target == null) {

            resetSprintState();
            return;
        }


        if (!shouldAttack())
            return;


        if (sprintReset.getValue()
                && mc.player.isSprinting()
                && !sprintResetDone
                && !shouldSkipSprintResetInWater()) {


            needSprintReset = true;
            return;
        }


        if (sprintReset.getValue()
                && sprintResetDone
                && sprintResetTicks < 1) {

            return;
        }


        attack();


        resetSprintState();
    }


    private boolean shouldAttack() {

        if (attackCooldown > 0)
            return false;


        if (!attackWhileEating.getValue()
                && mc.player.isUsingItem()
                && !mc.player.isBlocking()) {

            return false;
        }


        
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.93f)
            return false;


        
        if (this.hitDelay.getValue() > 0 && this.lastAttackAt > 0
                && System.currentTimeMillis() - this.lastAttackAt < (long) this.hitDelay.getValue())
            return false;



        
        if (smartCrits.getValue()) {

            boolean canCrit = idealHitUtils.canCritical(target);

            if (!canCrit)
                return false;
        }



        if (aimCheck.getValue()) {


            Vec3d eye =
                    mc.player.getCameraPosVec(1F);

            Vec3d look =
                    mc.player.getRotationVec(1F);


            EntityHitResult result =
                    ProjectileUtil.raycast(
                            mc.player,
                            eye,
                            eye.add(
                                    look.multiply(range.getFloatValue())
                            ),
                            mc.player.getBoundingBox()
                                    .expand(range.getFloatValue()),

                            e -> e != mc.player
                                    && e.isAlive(),

                            range.getFloatValue()
                                    * range.getFloatValue()
                    );


            if(result == null
                    || result.getEntity() != target) {

                return false;
            }
        }


        return true;
    }


    private LivingEntity getTargetUnderCrosshair() {


        Vec3d eye =
                mc.player.getCameraPosVec(1F);


        Vec3d look =
                mc.player.getRotationVec(1F);


        EntityHitResult hit =
                ProjectileUtil.raycast(
                        mc.player,
                        eye,
                        eye.add(
                                look.multiply(range.getFloatValue())
                        ),
                        mc.player.getBoundingBox()
                                .expand(range.getFloatValue()),
                        e -> e != mc.player
                                && e.isAlive()
                                && e instanceof LivingEntity,
                        range.getFloatValue()
                                * range.getFloatValue()
                );


        if (hit != null &&
                hit.getEntity() instanceof LivingEntity living
                && isValidTarget(living)) {

            return living;
        }


        return null;
    }


    private boolean isValidTarget(LivingEntity entity) {


        if (entity instanceof ArmorStandEntity)
            return false;


        if (entity instanceof PlayerEntity player) {


            if (!players.getValue())
                return false;


            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)
                    && !invisible.getValue())

                return false;


            if (Cheap.getInstance()
                    .getFriendManager()
                    .contains(player.getName().getString()))

                return false;


        } else if (entity instanceof PassiveEntity) {


            return passive.getValue();


        } else if (entity instanceof HostileEntity) {


            return hostile.getValue();
        }


        if (!throughWalls.getValue()
                && !mc.player.canSee(entity))

            return false;


        return true;
    }


    private void attack() {


        if (shieldBypass.getValue()
                && mc.player.isBlocking()) {

            mc.interactionManager.stopUsingItem(mc.player);
        }


        if (target instanceof PlayerEntity player
                && player.isBlocking()
                && shieldBreak.getValue()) {


            attackWithAxe(player);

        } else {


            mc.interactionManager.attackEntity(
                    mc.player,
                    target
            );


            mc.player.swingHand(
                    Hand.MAIN_HAND
            );
        }


        attackCooldown = 2;
        this.lastAttackAt = System.currentTimeMillis();
    }


    private void attackWithAxe(PlayerEntity player) {


        int slot = findAxeSlot();


        if (slot != -1) {


            int old =
                    mc.player.getInventory()
                            .selectedSlot;


            mc.player.getInventory()
                    .selectedSlot = slot;


            mc.interactionManager.attackEntity(
                    mc.player,
                    player
            );


            mc.player.swingHand(
                    Hand.MAIN_HAND
            );


            mc.player.getInventory()
                    .selectedSlot = old;


        } else {


            mc.interactionManager.attackEntity(
                    mc.player,
                    player
            );
        }
    }


    private int findAxeSlot() {


        for (int i = 0; i < 9; i++) {


            if (mc.player.getInventory()
                    .getStack(i)
                    .getItem() instanceof AxeItem)

                return i;
        }


        return -1;
    }


    private boolean shouldSkipSprintResetInWater() {


        return mc.player.isTouchingWater()
                && Sprint.INSTANCE != null
                && Sprint.INSTANCE.shouldKeepSprintInWater();
    }


    private void resetSprintState() {

        needSprintReset = false;
        sprintResetDone = false;
        sprintResetTicks = 0;
    }


    @Override
    public void onDisable() {

        super.onDisable();

        target = null;

        resetSprintState();

        attackCooldown = 0;
        lastAttackAt = -1L;
    }
}