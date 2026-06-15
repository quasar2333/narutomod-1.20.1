package net.narutomod.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.narutomod.world.VillagePoiHelper;

final class LegacyMerchantVillageBehavior {
    private int homeCheckTimer;

    void configureNavigation(PathfinderMob merchant) {
        if (merchant.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanPassDoors(true);
        }
    }

    void tickHomeRestriction(PathfinderMob merchant) {
        if (!(merchant.level() instanceof ServerLevel level)) {
            return;
        }
        if (--this.homeCheckTimer > 0) {
            return;
        }
        this.homeCheckTimer = 70 + merchant.getRandom().nextInt(50);
        VillagePoiHelper.findLegacyMerchantHomeContext(level, merchant.blockPosition())
                .ifPresentOrElse(
                        context -> merchant.restrictTo(context.center(), Math.max(1, (int) (context.radius() * 0.6F))),
                        merchant::clearRestriction);
    }

    static boolean isDefendVillageTarget(PathfinderMob merchant, LivingEntity candidate) {
        return isVillageAggressor(merchant, candidate) || isVillagerChaser(merchant, candidate);
    }

    static NearestAttackableTargetGoal<Player> targetPlayerGoal(PathfinderMob merchant) {
        return new LegacyVillageTargetPlayerGoal(merchant);
    }

    static boolean isVillagerChaser(PathfinderMob merchant, LivingEntity candidate) {
        if (!(candidate instanceof Zombie) || !(merchant.level() instanceof ServerLevel level)) {
            return false;
        }
        return VillagePoiHelper.findLegacyMerchantHomeContext(level, merchant.blockPosition())
                .filter(context -> candidate.blockPosition().distSqr(context.center())
                        <= (double) (context.radius() + 8) * (double) (context.radius() + 8))
                .map(context -> !level.getEntitiesOfClass(
                        Villager.class,
                        new AABB(candidate.blockPosition()).inflate(8.0D, 3.0D, 8.0D),
                        villager -> villager.isAlive()
                                && villager.blockPosition().distSqr(context.center())
                                <= (double) context.radius() * (double) context.radius())
                        .isEmpty())
                .orElse(false);
    }

    private static boolean isVillageAggressor(PathfinderMob merchant, LivingEntity candidate) {
        if (!(candidate instanceof Mob mob)
                || !(candidate instanceof Enemy)
                || candidate instanceof Creeper
                || !(merchant.level() instanceof ServerLevel level)
                || !(mob.getTarget() instanceof Villager villager)
                || !villager.isAlive()) {
            return false;
        }
        return VillagePoiHelper.findLegacyMerchantHomeContext(level, merchant.blockPosition())
                .filter(context -> isInVillageContext(mob, context) && isInVillageContext(villager, context))
                .isPresent();
    }

    private static boolean isVillageTargetPlayer(PathfinderMob merchant, Player candidate) {
        if (!(merchant.level() instanceof ServerLevel level)
                || !candidate.isAlive()
                || !LegacyMerchantOfferTiers.isVillageTargetPlayer(candidate)) {
            return false;
        }
        return VillagePoiHelper.findLegacyMerchantHomeContext(level, merchant.blockPosition())
                .filter(context -> isInVillageContext(candidate, context))
                .isPresent();
    }

    private static boolean isInVillageContext(LivingEntity entity, VillagePoiHelper.Context context) {
        return entity.blockPosition().distSqr(context.center())
                <= (double) (context.radius() + 8) * (double) (context.radius() + 8);
    }

    private static final class LegacyVillageTargetPlayerGoal extends NearestAttackableTargetGoal<Player> {
        private final PathfinderMob merchant;

        private LegacyVillageTargetPlayerGoal(PathfinderMob merchant) {
            super(merchant, Player.class, 10, false, false,
                    target -> target instanceof Player player && isVillageTargetPlayer(merchant, player));
            this.merchant = merchant;
        }

        @Override
        public boolean canUse() {
            return this.merchant.getRandom().nextInt(20) == 0 && super.canUse();
        }
    }
}
