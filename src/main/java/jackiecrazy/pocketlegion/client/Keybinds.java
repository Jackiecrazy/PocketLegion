package jackiecrazy.pocketlegion.client;

import com.mojang.blaze3d.platform.InputConstants;
import jackiecrazy.pocketlegion.PocketLegion;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = PocketLegion.MODID)
public class Keybinds {
    public static final KeyMapping LEGION = new KeyMapping("pocketlegion.legion", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.gameplay");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void handleInputEvent(InputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
//        if (BINDCAST.getKeyConflictContext().isActive() && BINDCAST.consumeClick() && mc.player.isAlive()) {
//            CombatChannel.INSTANCE.sendToServer(new EvokeSkillPacket());
//        }
    }
}
