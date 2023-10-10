package jackiecrazy.pocketlegion.api;

import jackiecrazy.pocketlegion.PocketLegion;
import jackiecrazy.pocketlegion.capability.Capabilities;
import jackiecrazy.pocketlegion.capability.summon.ISummon;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.LazyOptional;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

public class PetInfo {
    private CompoundTag pet;
    private Entity manifestation;
    private boolean fullRevive;
    private int incarnation;
    private int killTimer;
    private WeakReference<Entity> killer = new WeakReference<>(null);
    private LivingEntity summoner;
    private UUID summonerUID;

    private PetInfo() {

    }

    public PetInfo(LivingEntity pet, LivingEntity commander) {
        updatePet(pet);
        summoner = commander;
        summonerUID = commander.getUUID();
        manifestation = pet;
    }

    public static PetInfo read(CompoundTag tag) {
        PetInfo ret = new PetInfo();
        ret.pet = tag.getCompound("pet");
        ret.killTimer = tag.getInt("killTimer");
        ret.incarnation = tag.getInt("incarnation");
        ret.summonerUID = tag.getUUID("owner");
        return ret;
    }

    public void updatePet(Entity now) {
        CompoundTag tag = new CompoundTag();
        now.save(tag);
        this.pet = tag;
    }

    public int getKillTimer() {
        return killTimer;
    }

    public PetInfo setKillTimer(int killTimer) {
        this.killTimer = killTimer;
        return this;
    }

    public boolean decrementKillTimer() {
        if (killTimer > 0 && --killTimer == 0) {
            fullRevive = true;
            return true;
        }
        return false;
    }

    public Entity getKiller() {
        return killer.get();
    }

    public void setKiller(Entity killer) {
        this.killer = new WeakReference<>(killer);
    }

    public LivingEntity getSummoner(Level world) {
        if (summoner == null && summonerUID != null) {
            summoner = world.getPlayerByUUID(summonerUID);
        }
        return summoner;
    }

    public Component getName(){
        if (pet.contains("CustomName", 8)) {
            String s = pet.getString("CustomName");

            try {
                return Component.Serializer.fromJson(s);
            } catch (Exception exception) {
                pet.putString("CustomName", "Invalid");
                PocketLegion.LOGGER.warn("Failed to parse entity custom name {}", s, exception);
            }
            return Component.Serializer.fromJson(s);
        }
        final Optional<EntityType<?>> type = EntityType.by(pet);
        return type.map(EntityType::getDescription).orElseGet(Component::empty);
    }

    public Entity getOrCreateManifestation(Level world) {
        return manifestation != null && manifestation.level == world && !manifestation.isRemoved() ? manifestation : summon(world);
    }

    public Entity getManifestation(){
        return manifestation;
    }

    public Entity summon(Level world) {
        Optional<EntityType<?>> type = EntityType.by(pet);
        if (killTimer > 0) {
            System.out.println("time to revive: " + killTimer / 20);
        }
        if (type.isPresent() && killTimer <= 0) {
            Entity entity = type.get().create(world);
            assert entity != null;
            entity.load(pet);

            incarnation++;

            LazyOptional<ISummon> cap = entity.getCapability(Capabilities.PET, null);
            cap.ifPresent(a -> {
                a.setIncarnation(incarnation);
                a.setInfo(this);
                a.setSummoner(getSummoner(world));
                entity.setUUID(UUID.randomUUID());
                entity.clearFire();
            });
            if (fullRevive) {
                if (entity instanceof LivingEntity elb) {
                    elb.setHealth(elb.getMaxHealth());
                    elb.removeAllEffects();
                }
            }
            manifestation = entity;
            world.addFreshEntity(entity);
            return entity;
        }
        return null;
    }

    public void dismiss() {
        if (manifestation != null) {
            updatePet(manifestation);
            if(manifestation.level instanceof ServerLevel s){
                s.sendParticles(ParticleTypes.PORTAL, manifestation.getX(), manifestation.getY(), manifestation.getZ(), 30, 0, 0, 0, 1);
            }
            manifestation.remove(Entity.RemovalReason.DISCARDED);
        }
        manifestation = null;
    }

    public CompoundTag write(CompoundTag tag) {
        tag.put("pet", pet);
        tag.putInt("killTimer", killTimer);
        tag.putInt("incarnation", incarnation);
        tag.putUUID("owner", summonerUID);
        return tag;
    }

    public boolean invalid() {
        return EntityType.by(pet).isEmpty();
    }
}

