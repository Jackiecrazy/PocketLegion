package jackiecrazy.pocketlegion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class GuardGoal extends Goal {

    private final PathfinderMob mob;
    private final double speed;
    private final BlockPos homePos;
    private final double maxRadius;

    private int wanderCooldown = 0;

    public GuardGoal(PathfinderMob mob, BlockPos homePos, double maxRadius, double speed) {
        this.mob = mob;
        this.homePos = homePos;
        this.maxRadius = maxRadius;
        this.speed = speed;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private double distToHomeSqr() {
        return homePos.distToCenterSqr(mob.position());
    }

    @Override
    public boolean canUse() {
        if (wanderCooldown > 0) {
            wanderCooldown--;
            return false;
        }

        // If we're too far, we MUST go home
        if (distToHomeSqr() > maxRadius * maxRadius) {
            return true;
        }

        // Otherwise randomly decide to wander
        return mob.getRandom().nextInt(40) == 0; // ~once every 2 seconds
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        // Case 1: Too far → path directly home
        if (distToHomeSqr() > maxRadius * maxRadius) {
            mob.getNavigation().moveTo(
                    homePos.getX() + 0.5,
                    homePos.getY(),
                    homePos.getZ() + 0.5,
                    speed
            );
            return;
        }

        // Case 2: Wander near home
        Vec3 wanderTarget = getWanderPosNearHome();
        if (wanderTarget != null) {
            mob.getNavigation().moveTo(wanderTarget.x, wanderTarget.y, wanderTarget.z, speed);
        }

        wanderCooldown = 60 + mob.getRandom().nextInt(60); // 3–6s before next decision
    }

    private Vec3 getWanderPosNearHome() {
        for (int i = 0; i < 10; i++) {
            Vec3 vec = LandRandomPos.getPosTowards(mob, 10, 7, Vec3.atCenterOf(homePos));
            if(vec!=null){
                BlockPos pos = BlockPos.containing(vec.x, vec.y, vec.z);

                if (mob.level().isEmptyBlock(pos.above()) && mob.level().getBlockState(pos).isSolid()) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
        }

        return null;
    }
}
