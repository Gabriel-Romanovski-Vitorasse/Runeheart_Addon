package com.gabriel.runeheart;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/** Cliente do Mana Method: o que o servidor mandou sobre as runas do jogador, as teclas e o projétil. */
public final class ClientRunes {
    // padr\u00f5es escolhidos para n\u00e3o colidir com o ChronoBubble (V, B, K, Alt) nem com o Block Clover (1 a 8, G)
    public static final KeyBinding OPEN = new KeyBinding("key.runeheart.arrangements", KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.runeheart");
    public static final KeyBinding NEXT = new KeyBinding("key.runeheart.next_slot", KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.runeheart");
    public static final KeyBinding CAST = new KeyBinding("key.runeheart.cast", KeyConflictContext.IN_GAME,
            InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.runeheart");

    static int known, selected, maxRunes = 2, slots = 3, casts;
    static String[] texts = new String[RuneData.MAX_SLOTS];

    static {
        java.util.Arrays.fill(texts, "");
    }

    private ClientRunes() {
    }

    static void receive(RuneSyncPacket p) {
        known = p.known;
        selected = p.selected;
        maxRunes = p.maxRunes;
        slots = p.slots;
        casts = p.casts;
        java.util.Arrays.fill(texts, "");
        System.arraycopy(p.texts, 0, texts, 0, Math.min(p.texts.length, texts.length));
        if (Minecraft.getInstance().screen instanceof ArrangementScreen) {
            ((ArrangementScreen) Minecraft.getInstance().screen).onSync();
        }
    }

    static void send(int action, int slot, String text) {
        RuneheartMod.CHANNEL.sendToServer(new RuneActionPacket(action, slot, text));
    }

    @Mod.EventBusSubscriber(modid = RuneheartMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.registerKeyBinding(OPEN);
            ClientRegistry.registerKeyBinding(NEXT);
            ClientRegistry.registerKeyBinding(CAST);
            RenderingRegistry.registerEntityRenderingHandler(RuneheartMod.RUNE_BOLT.get(), BoltRenderer::new);
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
            if (mc.player == null || mc.getConnection() == null) return;
            while (OPEN.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new ArrangementScreen());
            }
            while (NEXT.consumeClick()) send(RuneActionPacket.NEXT, 0, "");
            while (CAST.consumeClick()) send(RuneActionPacket.CAST, 0, "");
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
            known = 0;
            selected = 0;
            casts = 0;
            java.util.Arrays.fill(texts, "");
        }
    }

    /** O projétil não tem modelo: o que se vê é o rastro de partículas que ele mesmo solta. */
    static final class BoltRenderer extends EntityRenderer<RuneBoltEntity> {
        private static final ResourceLocation NONE = new ResourceLocation("textures/misc/white.png");

        BoltRenderer(EntityRendererManager manager) {
            super(manager);
        }

        @Override
        public void render(RuneBoltEntity e, float yaw, float partial, MatrixStack ms, IRenderTypeBuffer buffers, int light) {
        }

        @Override
        public ResourceLocation getTextureLocation(RuneBoltEntity e) {
            return NONE;
        }
    }
}
