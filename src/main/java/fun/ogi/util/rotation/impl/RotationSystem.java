package fun.ogi.util.rotation.impl;

import fun.ogi.module.impl.list.combat.AttackAura;
import net.minecraft.entity.LivingEntity;

public abstract class RotationSystem {

    protected final AttackAura aura;

    protected RotationSystem(AttackAura aura) {
        this.aura = aura;
    }

    public abstract void update(LivingEntity target);
}

