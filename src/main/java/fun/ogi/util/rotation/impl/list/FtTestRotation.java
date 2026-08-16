package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;

public class FtTestRotation extends FtRotation {

    public FtTestRotation(AttackAura aura) {
        super(aura);
    }

    @Override
    protected boolean useCurrentPitch() {
        return false;
    }
}

