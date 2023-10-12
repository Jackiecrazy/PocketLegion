package jackiecrazy.pocketlegion.capability.summon;

import jackiecrazy.pocketlegion.api.PetInfo;
import jackiecrazy.pocketlegion.capability.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Summon implements ISummon, ICapabilitySerializable<CompoundTag>, ICapabilityProvider {
    private LivingEntity summoner;
    private UUID summonerUID;
    private PetInfo info;
    private int incarnation;

    @Override
    public LivingEntity getSummoner(Level w) {

        return summoner;
    }

    @Override
    public void setSummoner(LivingEntity to) {
        summoner = to;
        if (to == null) summonerUID = null;
        else summonerUID = to.getUUID();
    }

    @Override
    public PetInfo getInfo() {
        return info;
    }

    @Override
    public void setInfo(PetInfo link) {
        this.info = link;
    }

    @Override
    public void addTo(LivingEntity summoner) {

    }

    @Override
    public void clearLoyalty() {
        info = null;
    }

    @Override
    public void dismissed() {

    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return Capabilities.PET.orEmpty(cap, LazyOptional.of(() -> this));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag ret = new CompoundTag();
        if (summonerUID != null)
            ret.putUUID("summoner", summonerUID);
        return ret;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.hasUUID("summoner"))
            summonerUID = nbt.getUUID("summoner");
    }

    @Override
    public void setIncarnation(int incarnation) {
        this.incarnation = incarnation;
    }


    @Override
    public int getIncarnation() {
        return incarnation;
    }


}
