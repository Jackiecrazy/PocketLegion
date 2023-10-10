package jackiecrazy.pocketlegion.capability;

import jackiecrazy.pocketlegion.capability.commander.ICommander;
import jackiecrazy.pocketlegion.capability.summon.ISummon;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class Capabilities {
    public static final Capability<ICommander> COMMANDER = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static final Capability<ISummon> PET = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ICommander.class);
        event.register(ISummon.class);
    }
}
