package jackiecrazy.pocketlegion.capability.commander;

import jackiecrazy.pocketlegion.api.PetInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ICommander {
    Map<DyeColor, List<PetInfo>> getAllLegions();
    List<PetInfo> getLegion(DyeColor color);
    void summon(DyeColor color,LivingEntity commander, Level world);
    default void dismiss(){
        for(DyeColor d:DyeColor.values())
            dismiss(d);
    }
    void dismiss(DyeColor color);
    default boolean hasMember(LivingEntity member){
        for(DyeColor color:DyeColor.values())
            if(hasMember(member, color))
                return true;
        return false;
    }
    default boolean hasMember(LivingEntity member, DyeColor color){
        return getLegion(color).stream().anyMatch(a->a.getManifestation()==member);
    }
    PetInfo add(LivingEntity member, DyeColor color, LivingEntity commander);
    default void remove(LivingEntity member){
        for(DyeColor d:DyeColor.values())
            remove(member, d);
    }
    void remove(LivingEntity member, DyeColor color);

    CompoundTag write(CompoundTag tag);
    void read(CompoundTag from);

    void tick(LivingEntity commander);
}
