package com.gabriel.runeheart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/** Cliente: a tecla de concentração (absorção). Avisa o servidor quando ela é apertada e quando é solta. */
public final class ClientFocus {
    public static final KeyBinding FOCUS = new KeyBinding("key.runeheart.focus", InputMappings.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.runeheart");

    private static boolean sentDown;

    private ClientFocus() {
    }

    @Mod.EventBusSubscriber(modid = RuneheartMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.registerKeyBinding(FOCUS);
        }
    }

    @Mod.EventBusSubscriber(modid = RuneheartMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Ticks {
        private Ticks() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null) {
                sentDown = false;
                return;
            }
            // com uma tela aberta (inventário, chat) a tecla conta como solta
            boolean down = mc.screen == null && FOCUS.isDown();
            if (down == sentDown) return;
            sentDown = down;
            RuneheartMod.CHANNEL.sendToServer(new FocusPacket(down));
        }
    }
}
