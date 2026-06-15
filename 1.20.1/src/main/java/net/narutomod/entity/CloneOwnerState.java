package net.narutomod.entity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.narutomod.procedure.ProcedureUtils;

final class CloneOwnerState {
    private CloneOwnerState() {
    }

    static boolean isUnavailable(@Nullable LivingEntity owner) {
        return owner == null
                || !owner.isAlive()
                || owner.isSleeping()
                || ProcedureUtils.isPlayerDisconnected(owner);
    }
}
