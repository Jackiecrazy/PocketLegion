package jackiecrazy.pocketlegion.capability.summon;

import jackiecrazy.pocketlegion.api.PetInfo;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public interface ISummon {
    LivingEntity getSummoner(Level w);
    void setSummoner(LivingEntity to);
    PetInfo getInfo();
    void setInfo(PetInfo link);
    void addTo(LivingEntity summoner);
    void clearLoyalty();
    void dismissed();
    void setIncarnation(int incarnation);
    int getIncarnation();
}
