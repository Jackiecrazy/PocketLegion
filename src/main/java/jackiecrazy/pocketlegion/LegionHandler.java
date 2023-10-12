package jackiecrazy.pocketlegion;

import jackiecrazy.pocketlegion.api.PetInfo;
import jackiecrazy.pocketlegion.capability.Capabilities;
import jackiecrazy.pocketlegion.capability.commander.Commander;
import jackiecrazy.pocketlegion.capability.commander.ICommander;
import jackiecrazy.pocketlegion.capability.summon.ISummon;
import jackiecrazy.pocketlegion.capability.summon.Summon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
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
        if (event.getSource().getEntity() instanceof Player p && p.getMainHandItem().getItem() instanceof BannerItem banner && !p.level().isClientSide()) {
            LazyOptional<ICommander> sum = p.getCapability(Capabilities.COMMANDER, null);
            sum.ifPresent(a -> {
                a.summon(banner.getColor(), p, p.level());
            });
        }

    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract e) {
        if (e.getTarget() instanceof OwnableEntity ownable && e.getEntity().getMainHandItem().getItem() instanceof BannerItem banner) {
            Entity entity = e.getTarget();
            if (ownable.getOwner() == e.getEntity() && entity instanceof LivingEntity pet && ownable.getOwner() !=null) {
                LivingEntity owner=ownable.getOwner();
                LazyOptional<ICommander> sum = ownable.getOwner().getCapability(Capabilities.COMMANDER, null);
                LazyOptional<ISummon> cap = e.getTarget().getCapability(Capabilities.PET, null);
                if (cap.isPresent() && cap.resolve().get().getSummoner(pet.level()) != null && cap.resolve().get().getSummoner(pet.level()) != owner) {
                    e.getEntity().displayClientMessage(Component.translatable("pocketlegion.another"), true);
                    return;
                }
                PetInfo link = null;
                if (sum.isPresent()) {
                    final ICommander commander = sum.resolve().get();
                    if(commander.hasMember(pet, banner.getColor())){
                        e.getEntity().displayClientMessage(Component.translatable("pocketlegion.already"), true);
                        return;
                    }
                    if(commander.hasMember(pet))
                        commander.remove(pet);
                    link = commander.add(pet, banner.getColor(), owner);
                }
                if (cap.isPresent()) {
                    cap.resolve().get().setSummoner(owner);
                    cap.resolve().get().setInfo(link);
                }
                e.getEntity().displayClientMessage(Component.translatable("pocketlegion.added", banner.getColor().getName()), true);
            }
        }
    }



    @SubscribeEvent
    public static void sleep(PlayerWakeUpEvent e) {
        LazyOptional<ICommander> sum = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        sum.ifPresent(command->{
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
        LazyOptional<ISummon> sum = e.getEntity().getCapability(Capabilities.PET, null);
        sum.ifPresent(a -> {
            if (a.getInfo() != null && e.getSource().getEntity() instanceof LivingEntity) {
                a.getInfo().setKiller(e.getSource().getEntity());
                a.getInfo().setKillTimer(1200);
                a.getInfo().dismiss();
            }
        });
        LazyOptional<ICommander> cap = e.getEntity().getCapability(Capabilities.COMMANDER, null);
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
        cap.ifPresent(a->a.tick(e.getEntity()));
    }

    @SubscribeEvent
    public static void dieCD(PlayerEvent.PlayerRespawnEvent e) {
        if (e.isEndConquered()) return;
        LazyOptional<ICommander> cap = e.getEntity().getCapability(Capabilities.COMMANDER, null);
        cap.ifPresent(a -> {
            a.getAllLegions().forEach((color, pets)-> pets.forEach(b -> {
                b.dismiss();
                b.setKillTimer(1200);
            }));
        });
    }


}
