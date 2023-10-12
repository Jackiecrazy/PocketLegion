package jackiecrazy.pocketlegion.capability.commander;

import jackiecrazy.pocketlegion.PocketLegion;
import jackiecrazy.pocketlegion.api.PetInfo;
import jackiecrazy.pocketlegion.capability.Capabilities;
import jackiecrazy.pocketlegion.capability.summon.ISummon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commander implements ICommander, ICapabilitySerializable<CompoundTag>, ICapabilityProvider {
    private final HashMap<DyeColor, List<PetInfo>> legions = new HashMap<>();
    private int sneakTime = 0;

    @Override
    public Map<DyeColor, List<PetInfo>> getAllLegions() {
        for (DyeColor color : DyeColor.values())
            legions.putIfAbsent(color, new ArrayList<>());
        return legions;
    }

    @Override
    public List<PetInfo> getLegion(DyeColor color) {
        legions.putIfAbsent(color, new ArrayList<>());
        return legions.get(color);
    }

    @Override
    public void summon(DyeColor color, LivingEntity commander, Level world) {
        for (PetInfo pet : getLegion(color)) {
            if (pet.invalid()) {
                PocketLegion.LOGGER.error("legion member " + pet.getName() + " is invalid!");
            }
            if (pet.getKiller() instanceof LivingEntity elb) {
                elb.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
            }
            final Entity manifestation = pet.getOrCreateManifestation(world);
            if (manifestation != null) {
                BlockPos blockpos = commander.blockPosition();
                boolean flag = false;
                for (int i = 0; i < 10; ++i) {
                    int xMod = this.randomIntInclusive(manifestation, -3, 3);
                    int yMod = this.randomIntInclusive(manifestation, -1, 1);
                    int zMod = this.randomIntInclusive(manifestation, -3, 3);
                    flag = this.maybeTeleportTo(manifestation, commander, blockpos.getX() + xMod, blockpos.getY() + yMod, blockpos.getZ() + zMod);
                    if (flag) {
                        break;
                    }
                }
                if (!flag) {
                    manifestation.setPos(commander.position());
                }
            }
        }
    }

    @Override
    public void dismiss(DyeColor color) {
        for (PetInfo pet : getLegion(color)) {
            pet.dismiss();
        }
    }

    @Override
    public PetInfo add(LivingEntity member, DyeColor color, LivingEntity commander) {
        final PetInfo e = new PetInfo(member, commander);
        legions.get(color).add(e);
        return e;
    }

    @Override
    public void remove(LivingEntity member, DyeColor color) {
        if (getLegion(color).removeIf(a -> a.getManifestation() == member)) {
            LazyOptional<ISummon> sum = member.getCapability(Capabilities.PET, null);
            sum.ifPresent(a -> {
                a.setInfo(null);
                a.setSummoner(null);
            });
        }
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        for (DyeColor color : DyeColor.values()) {
            ListTag list = new ListTag();
            for (PetInfo pet : getLegion(color)) {
                list.add(pet.write(new CompoundTag()));
            }
            tag.put(color.getName(), list);
        }
        return tag;
    }

    @Override
    public void read(CompoundTag from) {
        legions.clear();
        for (DyeColor d : DyeColor.values()) {
            ListTag list = from.getList(d.getName(), CompoundTag.TAG_COMPOUND);
            for (int a = 0; a < list.size(); a++) {
                CompoundTag ct = list.getCompound(a);
                getLegion(d).add(PetInfo.read(ct));
            }
        }
    }

    @Override
    public void tick(LivingEntity commander) {
        getAllLegions().forEach((color, pets) -> {
            for (PetInfo pet : pets) {
                if ((pet.getKiller() == null || !pet.getKiller().isAlive()) && pet.decrementKillTimer() && commander instanceof Player p) {
                    boolean all = pets.stream().noneMatch(a -> a.getKillTimer() > 0);
                    if (all)
                        p.displayClientMessage(Component.translatable("pocketlegion.revived.all", color.getName()), true);
                    else
                        p.displayClientMessage(Component.translatable("pocketlegion.revived", pet.getName(), color.getName()), true);
                }

                if (sneakTime > 0 && pet.getManifestation() != null && pet.getManifestation().level() instanceof ServerLevel s) {
                    s.sendParticles(ParticleTypes.PORTAL, pet.getManifestation().getX(), pet.getManifestation().getY(), pet.getManifestation().getZ(), 1, 0, 0, 0, 1);
                }
            }
        });

        if (commander.isShiftKeyDown() && commander.getMainHandItem().getItem() instanceof BannerItem banner) {
            if (++sneakTime > 60) {
                commander.level().playSound(null, commander.getX(), commander.getY(), commander.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f);
                dismiss(banner.getColor());
                sneakTime = -Integer.MAX_VALUE;
            }
        } else sneakTime = 0;
    }

    private int randomIntInclusive(Entity e, int from, int to) {
        return PocketLegion.rand.nextInt(to - from + 1) + from;
    }

    private boolean maybeTeleportTo(Entity e, LivingEntity owner, int x, int y, int z) {
        if (Math.abs((double) x - owner.getX()) < 2.0D && Math.abs((double) z - owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(e, new BlockPos(x, y, z))) {
            return false;
        } else {
            if (e.position().distanceToSqr(owner.position()) <= 100) {
                if (e instanceof Mob mob)
                    mob.getNavigation().moveTo(x, y, z, 1);
            } else {
                e.teleportTo(x, y, z);
                if (e.level() instanceof ServerLevel s) {
                    s.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY(), e.getZ(), 30, 0, 0, 0, 1);
                }
                if (e instanceof Mob mob)
                    mob.getNavigation().moveTo(x, y, z, 1);
            }
            return true;
        }
    }

    private boolean canTeleportTo(@NotNull Entity pet, BlockPos pos) {
        BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(pet.level(), pos.mutable());
        if (blockpathtypes != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = pet.level().getBlockState(pos.below());
            if (blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pos.subtract(pet.blockPosition());
                return pet.level().noCollision(pet, pet.getBoundingBox().move(blockpos));
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return Capabilities.COMMANDER.orEmpty(cap, LazyOptional.of(() -> this));
    }

    @Override
    public CompoundTag serializeNBT() {
        return write(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        read(nbt);
    }
}
