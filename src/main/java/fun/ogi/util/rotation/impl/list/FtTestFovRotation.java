package fun.ogi.util.rotation.impl.list;

import fun.ogi.module.impl.list.combat.AttackAura;

public class FtTestFovRotation extends FtRotation {

    public FtTestFovRotation(AttackAura aura) {
        super(aura);
    }

    @Override
    protected boolean useCurrentPitch() {
        return true;
    }
}

