package jackiecrazy.pocketlegion.capability.commander;

import jackiecrazy.pocketlegion.api.PetInfo;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ICommander {
    HashMap<LivingEntity, List<Mob>> targets = new HashMap<>();
    List<Mob> available = new ArrayList<>();

    Map<DyeColor, List<PetInfo>> getAllLegions();

    List<PetInfo> getLegion(DyeColor color);

    void summon(DyeColor color, LivingEntity commander, Level world);

    default void dismiss() {
        for (DyeColor d : DyeColor.values())
            dismiss(d);
    }

    default void guard(DyeColor d, BlockPos bp){
        getLegion(d).forEach(a->a.guard(bp));
    }

    default void follow(DyeColor d){
        getLegion(d).forEach(PetInfo::follow);
    }

    //rules for aggro are simple:
    // when hitting or being hit by an entity while holding the flag of the legion:
    // look through all legion members and their targets and collect them in a map,
    // find maximum number of same legion members attacking the same mob
    // look for the closest mobs with non-unique targets and send them over
    default void shiftAggro(DyeColor banner, LivingEntity onto) {
        int most = 0;
        for (PetInfo info : getLegion(banner)) {
            if (info.getManifestation() instanceof Mob e) {
                if (e.getTarget() == null) available.add(e);
                else {
                    targets.putIfAbsent(e.getTarget(), new ArrayList<>());
                    targets.get(e.getTarget()).add(e);
                    final int unique = targets.get(e.getTarget()).size();
                    most = Math.max(unique, most);
                }
            }
        }
        most /= 2;
        if (most > 0) {
            //find the fastest mobs that aren't targeting a unique target
            for (Map.Entry<LivingEntity, List<Mob>> entry : targets.entrySet()) {
                if (entry.getValue().size() > 1) {
                    Mob slowest = null;
                    for (Mob pet : entry.getValue()) {
                        //new slowest
                        if (slowest == null || slowest.getAttributeValue(Attributes.MOVEMENT_SPEED) > slowest.getAttributeValue(Attributes.MOVEMENT_SPEED)) {
                            slowest = pet;
                        }
                    }
                    for (Mob pet : entry.getValue()) {
                        if (pet != slowest)
                            available.add(pet);
                    }
                }
            }
            //sort for the closest ones
            available.sort((a, b) -> (int) (onto.distanceToSqr(b) - onto.distanceToSqr(a)));
            for (int x = 0; x < Math.min(most, available.size()); x++) {
                available.get(x).setTarget(onto);
            }
        }
        //done using, throw them away
        targets.clear();
        available.clear();
    }

    void dismiss(DyeColor color);

    default boolean hasMember(LivingEntity member) {
        for (DyeColor color : DyeColor.values())
            if (hasMember(member, color)) return true;
        return false;
    }

    default boolean hasMember(LivingEntity member, DyeColor color) {
        return getLegion(color).stream().anyMatch(a -> a.getManifestation() == member);
    }

    PetInfo add(LivingEntity member, DyeColor color, LivingEntity commander);

    default void remove(LivingEntity member) {
        for (DyeColor d : DyeColor.values())
            remove(member, d);
    }

    void remove(LivingEntity member, DyeColor color);

    CompoundTag write(CompoundTag tag);

    void read(CompoundTag from);

    void tick(LivingEntity commander);
}
