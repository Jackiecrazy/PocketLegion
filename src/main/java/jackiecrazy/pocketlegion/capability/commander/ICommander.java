package jackiecrazy.pocketlegion.capability.commander;

import jackiecrazy.pocketlegion.api.PetInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;

public interface ICommander {
    List<PetInfo> getLegion();
    void summon(LivingEntity commander, Level world);
    void dismiss();
    PetInfo add(LivingEntity member, LivingEntity commander);
    void remove(LivingEntity member);

    CompoundTag write(CompoundTag tag);
    void read(CompoundTag from);

    void tick(LivingEntity commander);
}
