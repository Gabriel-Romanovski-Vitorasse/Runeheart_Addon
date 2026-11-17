package com.gabriel.runeheart;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Cliente: o medidor de mana natural (só com o Mana Sense ligado) e o pólen brilhante nos lugares densos.
 * Os valores chegam do servidor a cada meio segundo ({@link DensityPacket}).
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientNaturalMana {
    /** Sem pacote por mais que isso (ticks), o medidor some (ex.: mana natural desligada no servidor). */
    private static final int STALE_TICKS = 40;
    /** A tendência compara com o valor de alguns pacotes atrás, para não piscar a cada mudança pequena. */
    private static final int TREND_WINDOW = 4;

    private static int density, capacity;
    private static boolean sensing;
    private static long receivedAt = -1000;
    private static final int[] HISTORY = new int[TREND_WINDOW];
    private static int historyCount;
    private static final Random RANDOM = new Random();

    private ClientNaturalMana() {
    }

    static void receive(int newDensity, int newCapacity, boolean newSensing) {
        System.arraycopy(HISTORY, 1, HISTORY, 0, TREND_WINDOW - 1);
        HISTORY[TREND_WINDOW - 1] = newDensity;
        historyCount = Math.min(TREND_WINDOW, historyCount + 1);
        density = newDensity;
        capacity = newCapacity;
        sensing = newSensing;
        ClientWorld world = Minecraft.getInstance().level;
        receivedAt = world == null ? 0 : world.getGameTime();
    }

    private static boolean fresh() {
        ClientWorld world = Minecraft.getInstance().level;
        return world != null && world.getGameTime() - receivedAt <= STALE_TICKS;
    }

    /** -1 caindo, 0 parado, 1 subindo. */
    private static int trend() {
        if (historyCount < TREND_WINDOW) return 0;
        int d = HISTORY[TREND_WINDOW - 1] - HISTORY[0];
        return d >= 1 ? 1 : (d <= -1 ? -1 : 0);
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (!RuneConfig.SHOW_HUD.get() || !sensing || !fresh() || mc.options.hideGui || mc.player == null) return;
        MatrixStack ms = event.getMatrixStack();
        FontRenderer font = mc.font;
        int x = RuneConfig.HUD_X.get(), y = RuneConfig.HUD_Y.get();
        int width = 82;

        String arrow = trend() > 0 ? " ▲" : (trend() < 0 ? " ▼" : "");
        String label = new TranslationTextComponent("hud.runeheart.natural_mana").getString();
        font.drawShadow(ms, label + " " + density + arrow, x, y, 0xB6F2A8);

        // barra: o cheio do lugar em cinza, o que tem agora em verde
        int barY = y + 11;
        AbstractGui.fill(ms, x, barY, x + width, barY + 4, 0x90000000);
        AbstractGui.fill(ms, x, barY, x + width * Math.min(100, capacity) / 100, barY + 4, 0x6050705A);
        AbstractGui.fill(ms, x, barY, x + width * Math.min(100, density) / 100, barY + 4, 0xFF7BD86A);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null || mc.player == null || !fresh()) return;
        if (!RuneConfig.PARTICLES.get()) return;
        int threshold = RuneConfig.PARTICLE_THRESHOLD.get();
        if (density < threshold) return;
        // quanto mais acima do limite, mais pólen: de 1 a cada 4 ticks até 2 por tick
        float strength = (density - threshold + 1) / (float) Math.max(1, 101 - threshold);
        int count = strength > 0.75F ? 2 : (strength > 0.4F || mc.level.getGameTime() % 4 == 0 ? 1 : 0);
        PlayerEntity p = mc.player;
        for (int i = 0; i < count; i++) {
            double px = p.getX() + (RANDOM.nextDouble() - 0.5D) * 16.0D;
            double py = p.getY() + RANDOM.nextDouble() * 4.0D;
            double pz = p.getZ() + (RANDOM.nextDouble() - 0.5D) * 16.0D;
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 0.0D, 0.01D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        sensing = false;
        receivedAt = -1000;
        historyCount = 0;
    }
}
