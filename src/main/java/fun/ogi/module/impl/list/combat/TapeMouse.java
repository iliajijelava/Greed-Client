package fun.ogi.module.impl.list.combat;

import com.google.common.eventbus.Subscribe;
import fun.ogi.Cheap;
import fun.ogi.events.render.EventUpdate;
import fun.ogi.module.Module;
import fun.ogi.module.ModuleCategory;
import fun.ogi.module.ModuleInformation;
import fun.ogi.module.settings.BooleanSetting;
import fun.ogi.module.settings.SliderSetting;
import fun.ogi.util.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "TapeMouse",moduleDesc = "Automatically hits in your delay ",moduleCategory = ModuleCategory.COMBAT)
public class TapeMouse extends Module {
    private final SliderSetting delay = new SliderSetting("Attack Delay (second)",this,0.5,0.1,10,0.05);
    private final BooleanSetting players =
            new BooleanSetting("Players", this, true);

    private final BooleanSetting invisible =
            new BooleanSetting("Invisible", this, true);

    private final BooleanSetting passive =
            new BooleanSetting("Passive", this, false);

    private final BooleanSetting hostile =
            new BooleanSetting("Hostile", this, true);
    private final BooleanSetting throughWalls = new BooleanSetting("Through walls",this,false);
    private Timer timer = new Timer();
    public TapeMouse(){
        addSettings(delay,players,invisible,passive,hostile,throughWalls);
    }
    @Subscribe
    private void onEventUpdate(EventUpdate e){
        if(mc.player==null || mc.world == null)return;
        long attackCd = (long) (delay.getValue() * 1000);
        if(timer.finished(attackCd)){
            LivingEntity target = getTargetUnderCrosshair();
            if (target == null) return;
            mc.interactionManager.attackEntity(
                    mc.player,
                    target
            );

            timer.reset();
        }
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
                                look.multiply(3f)
                        ),
                        mc.player.getBoundingBox()
                                .expand(3f),
                        e -> e != mc.player
                                && e.isAlive()
                                && e instanceof LivingEntity,
                        3f
                                * 3f
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
}

