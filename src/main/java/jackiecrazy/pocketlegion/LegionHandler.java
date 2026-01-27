package jackiecrazy.pocketlegion;

import jackiecrazy.pocketlegion.ai.GuardGoal;
import jackiecrazy.pocketlegion.api.PetInfo;
import jackiecrazy.pocketlegion.capability.Capabilities;
import jackiecrazy.pocketlegion.capability.commander.Commander;
import jackiecrazy.pocketlegion.capability.commander.ICommander;
import jackiecrazy.pocketlegion.capability.summon.ISummon;
import jackiecrazy.pocketlegion.capability.summon.Summon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PocketLegion.MODID)
public class LegionHandler {

    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player)
            event.addCapability(new ResourceLocation(PocketLegion.MODID, "commander"), new Commander());

        else
            event.addCapability(new ResourceLocation(PocketLegion.MODID, "summon"), new Summon());

    }

    @SubscribeEvent
    public static void test(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity e) {
            if (event.getSource().getEntity() instanceof Player p && p.getMainHandItem().getItem() instanceof BannerItem banner && !p.level().isClientSide()) {
                LazyOptional<ICommander> sum = p.getCapability(Capabilities.COMMANDER, null);
                sum.ifPresent(a -> {
                    final DyeColor color = banner.getColor();
                    a.follow(color);
                    a.summon(color, p, p.level());
                    a.shiftAggro(color, event.getEntity());
                });
            } else if (event.getEntity() instanceof Player p && !p.level().isClientSide()) {
                LazyOptional<ICommander> sum = p.getCapability(Capabilities.COMMANDER, null);
                sum.ifPresent(a -> {
                    if (p.getMainHandItem().getItem() instanceof BannerItem banner) {
                        final DyeColor color = banner.getColor();
                        a.follow(color);
                        a.summon(color, p, p.level());
                        a.shiftAggro(color, e);
                    }
                    if (p.getOffhandItem().getItem() instanceof BannerItem banner) {
                        final DyeColor color = banner.getColor();
                        a.follow(color);
                        a.summon(color, p, p.level());
                        a.shiftAggro(color, e);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void justChill(PlayerInteractEvent.RightClickBlock e) {
        if (!e.getLevel().isClientSide() && e.getHand() == InteractionHand.MAIN_HAND && e.getItemStack().isEmpty() && e.getLevel().getBlockState(e.getPos()).getBlock() instanceof BannerBlock b) {
            LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
            sum.ifPresent(a -> {
                a.summon(b.getColor(), e.getEntity(), e.getLevel());
                for (PetInfo p : a.getLegion(b.getColor())) {
                    p.guard(e.getPos());
                }
            });
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void justChill(PlayerInteractEvent.RightClickItem e) {
        if (!e.getLevel().isClientSide() && e.getHand() == InteractionHand.MAIN_HAND && e.getItemStack().getItem() instanceof BannerItem b) {
            LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
            sum.ifPresent(a -> {
                for (PetInfo p : a.getLegion(b.getColor())) {
                    String status = p.getKillTimer() > 0 ? ChatFormatting.RED + "" : "";
                    status = status.concat(p.getName().getString()) + ": " + p.getHealthForDisplay() + ChatFormatting.RESET;
                    e.getEntity().displayClientMessage(Component.literal(status), false);
                }
            });
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (e.getTarget() instanceof OwnableEntity ownable && e.getEntity().getMainHandItem().getItem() instanceof BannerItem banner) {
            Entity entity = e.getTarget();
            if (ownable.getOwner() == e.getEntity() && entity instanceof LivingEntity pet && ownable.getOwner() != null) {
                LivingEntity owner = ownable.getOwner();
                LazyOptional<ICommander> sum = ownable.getOwner().getCapability(Capabilities.COMMANDER, null);
                LazyOptional<ISummon> cap = e.getTarget().getCapability(Capabilities.PET, null);
                if (cap.isPresent() && cap.resolve().get().getSummoner(pet.level()) != null && cap.resolve().get().getSummoner(pet.level()) != owner) {
                    e.getEntity().displayClientMessage(Component.translatable("pocketlegion.another"), true);
                    e.setResult(Event.Result.DENY);
                    return;
                }
                PetInfo link = null;
                if (sum.isPresent()) {
                    final ICommander commander = sum.resolve().get();
                    if (commander.hasMember(pet, banner.getColor())) {
                        e.getEntity().displayClientMessage(Component.translatable("pocketlegion.already"), true);
                        e.setResult(Event.Result.DENY);
                        return;
                    }
                    if (commander.hasMember(pet) && owner.isShiftKeyDown())
                        commander.remove(pet);
                    link = commander.add(pet, banner.getColor(), owner);
                }
                if (cap.isPresent()) {
                    cap.resolve().get().setSummoner(owner);
                    cap.resolve().get().setInfo(link);
                }
                e.getEntity().displayClientMessage(Component.translatable("pocketlegion.added", banner.getColor().getName()), true);
                e.setResult(Event.Result.ALLOW);
            }
        }
    }


    @SubscribeEvent
    public static void sleep(PlayerWakeUpEvent e) {
        LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        sum.ifPresent(command -> {
            command.getAllLegions().forEach((color, pets) -> {
                for (PetInfo pet : pets) {
                    pet.setKiller(null);
                    pet.setKillTimer(1);
                    pet.decrementKillTimer();
                }
            });
            e.getEntity().displayClientMessage(Component.translatable("pocketlegion.revived.all.all"), true);
        });

    }

    @SubscribeEvent
    public static void dismiss(PlayerEvent.PlayerChangedDimensionEvent e) {
        LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        sum.ifPresent(ICommander::dismiss);
    }

    @SubscribeEvent
    public static void dismiss(PlayerEvent.PlayerLoggedOutEvent e) {
        LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        sum.ifPresent(ICommander::dismiss);
    }

    @SubscribeEvent
    public static void dismiss(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk()) {
            LazyOptional<ISummon> sum = e.getEntity().getCapability(Capabilities.PET, null);
            sum.ifPresent(a -> {
                if (a.getSummoner(e.getLevel()) != null)
                    e.setCanceled(true);
            });
        }
    }

    @SubscribeEvent
    public static void dieCD(LivingDeathEvent e) {
        final LivingEntity dying = e.getEntity();
        LazyOptional<ISummon> sum = dying.getCapability(Capabilities.PET, null);
        sum.ifPresent(a -> {
            if (a.getInfo() != null) {
                final LivingEntity summoner = a.getSummoner(dying.level());
                a.getInfo().dismiss();
                if (e.getSource().getEntity() instanceof LivingEntity s && s != summoner) {
                    a.getInfo().setKiller(s);
                }
                a.getInfo().setKillTimer(1200);
                if (summoner instanceof ServerPlayer sp) {
                    sp.displayClientMessage(e.getSource().getLocalizedDeathMessage(dying), false);
                }
            }
        });
        LazyOptional<ICommander> cap = dying.getCapability(Capabilities.COMMANDER, null);
        cap.ifPresent(ICommander::dismiss);
    }

    @SubscribeEvent
    public static void dismissal(LivingEvent.LivingTickEvent e) {
        LazyOptional<ISummon> sum = e.getEntity().getCapability(Capabilities.PET, null);
        sum.ifPresent(a -> {
            if (a.getInfo() != null && (a.getSummoner(e.getEntity().level()) == null || !a.getSummoner(e.getEntity().level()).isAlive())) {
                a.getInfo().dismiss();
            }
        });
        LazyOptional<ICommander> cap = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        cap.ifPresent(a -> a.tick(e.getEntity()));
    }

    @SubscribeEvent
    public static void dieCD(PlayerEvent.PlayerRespawnEvent e) {
        if (e.isEndConquered()) return;
        LazyOptional<ICommander> cap = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        cap.ifPresent(a -> {
            a.getAllLegions().forEach((color, pets) -> pets.forEach(b -> {
                b.dismiss();
                b.setKillTimer(1200);
            }));
        });
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        final Player orig = e.getOriginal();
        final Player p = e.getEntity();
        orig.reviveCaps();
        LazyOptional<ICommander> fromCap = orig.getCapability(Capabilities.COMMANDER, null);
        LazyOptional<ICommander> toCap = p.getCapability(Capabilities.COMMANDER, null);
        fromCap.ifPresent(from -> {
            toCap.ifPresent(to -> to.read(from.write(new CompoundTag())));
        });
        orig.invalidateCaps();
    }


}
