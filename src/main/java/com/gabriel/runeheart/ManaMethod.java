package com.gabriel.runeheart;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conjuração do Mana Method. O jogador aperta a tecla de conjurar: a mana de ignição sai na hora e um círculo de
 * runas se completa enquanto ele canaliza (0,5 s por runa). Levar dano no meio quebra o arranjo e a ignição se
 * perde. No fim, a força vem da mana natural do lugar; num lugar seco demais o arranjo se desfaz.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManaMethod {
    private static final class Channel {
        final Arrangement arrangement;
        final int total;
        int left;

        Channel(Arrangement arrangement) {
            this.arrangement = arrangement;
            this.total = arrangement.channelTicks();
            this.left = total;
        }
    }

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private ManaMethod() {
    }

    static void onAction(ServerPlayerEntity player, int action, int slot, String text) {
        switch (action) {
            case RuneActionPacket.SAVE: save(player, slot, text); break;
            case RuneActionPacket.SELECT: select(player, slot); break;
            case RuneActionPacket.NEXT: select(player, RuneData.selected(player) + 1); break;
            case RuneActionPacket.CAST: cast(player); break;
            default: break;
        }
    }

    private static void save(ServerPlayerEntity player, int slot, String text) {
        if (slot < 0 || slot >= RuneData.slots(player)) return;
        Arrangement a = Arrangement.parse(text);
        if (a != null) {
            String problem = a.problem(RuneData.maxRunes(player), RuneData.known(player));
            if (problem != null) {
                Msg.bar(player, TextFormatting.RED, problem, RuneData.maxRunes(player));
                RuneData.sync(player);
                return;
            }
        }
        RuneData.setSlot(player, slot, a);
        RuneData.select(player, slot);
        RuneData.sync(player);
    }

    private static void select(ServerPlayerEntity player, int slot) {
        RuneData.select(player, slot);
        RuneData.sync(player);
        int now = RuneData.selected(player);
        Arrangement a = RuneData.slot(player, now);
        if (a == null) Msg.bar(player, TextFormatting.GRAY, "slot_empty", now + 1);
        else Msg.bar(player, TextFormatting.AQUA, "slot_selected", now + 1, names(a));
    }

    /** "Disparo + Potência" como texto traduzível. */
    static net.minecraft.util.text.IFormattableTextComponent names(Arrangement a) {
        net.minecraft.util.text.IFormattableTextComponent t = new net.minecraft.util.text.StringTextComponent("");
        boolean first = true;
        for (Rune r : a.runes()) {
            if (!first) t.append(" + ");
            t.append(new net.minecraft.util.text.TranslationTextComponent(r.langKey()));
            first = false;
        }
        return t;
    }

    private static void cast(ServerPlayerEntity player) {
        if (!RuneConfig.MM_ENABLED.get()) {
            Msg.bar(player, TextFormatting.GRAY, "mm_disabled");
            return;
        }
        if (CHANNELS.containsKey(player.getUUID())) return;                      // já canalizando
        long now = player.level.getGameTime();
        Long ready = READY_AT.get(player.getUUID());
        if (ready != null && now < ready) {
            Msg.bar(player, TextFormatting.GRAY, "mm_cooldown", Msg.f1((ready - now) / 20.0D));
            return;
        }
        Arrangement a = RuneData.slot(player, RuneData.selected(player));
        if (a == null) {
            Msg.bar(player, TextFormatting.GRAY, "slot_empty", RuneData.selected(player) + 1);
            return;
        }
        String problem = a.problem(RuneData.maxRunes(player), RuneData.known(player));
        if (problem != null) {
            Msg.bar(player, TextFormatting.RED, problem, RuneData.maxRunes(player));
            return;
        }
        float max = BlockCloverBridge.getMaxMana(player);
        if (max <= 0.0F) {
            Msg.bar(player, TextFormatting.GRAY, "abs_no_mana_stats");
            return;
        }
        float ignition = max * RuneConfig.f(RuneConfig.MM_IGNITION_PERCENT) / 100.0F;
        if (BlockCloverBridge.getMana(player) < ignition) {
            Msg.bar(player, TextFormatting.RED, "mm_no_mana");
            return;
        }
        BlockCloverBridge.addMana(player, -ignition);
        BlockCloverBridge.syncToClient(player);
        CHANNELS.put(player.getUUID(), new Channel(a));
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 0.8F, 1.3F);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        Channel c = CHANNELS.get(player.getUUID());
        if (c == null) return;
        if (!player.isAlive()) { CHANNELS.remove(player.getUUID()); return; }
        c.left--;
        circle(player, c);
        if (c.left > 0) {
            if (c.left % 5 == 0) Msg.bar(player, TextFormatting.AQUA, "mm_channel", bar(c.total - c.left, c.total));
            return;
        }
        CHANNELS.remove(player.getUUID());
        READY_AT.put(player.getUUID(), player.level.getGameTime() + RuneConfig.MM_COOLDOWN.get());
        finish(player, c.arrangement);
    }

    private static void finish(ServerPlayerEntity player, Arrangement a) {
        ServerWorld world = player.getLevel();
        float density = NaturalMana.density(world, player.blockPosition());
        if (density < RuneConfig.MM_FIZZLE_BELOW.get()) {
            Msg.bar(player, TextFormatting.RED, "mm_fizzle", Math.round(density));
            world.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.4D, 0.4D, 0.02D);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.6F, 1.4F);
            return;
        }
        NaturalMana.drain(world, player.blockPosition(), RuneConfig.f(RuneConfig.MM_NATURAL_COST) * a.size());
        float strength = RuneMath.strength(density, a.has(Rune.POWER), RuneConfig.f(RuneConfig.MM_POWER_MULT));
        boolean done = a.has(Rune.TRIGGER) ? RuneTraps.place(player, a, strength) : RuneEffects.cast(player, a, strength);
        if (!done) return;
        int before = RuneData.casts(player);
        RuneData.setCasts(player, before + 1);
        if (Mastery.levelsUp(before, RuneConfig.MM_MASTERY_THREE.get(), RuneConfig.MM_MASTERY_FIVE.get())) {
            Msg.chat(player, TextFormatting.GOLD, "mastery_up", RuneData.maxRunes(player), RuneData.slots(player));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.2F);
        }
        RuneData.sync(player);
    }

    private static String bar(int done, int total) {
        int filled = Math.round(10.0F * done / Math.max(1, total));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 10; i++) b.append(i < filled ? '■' : '□');
        return b.toString();
    }

    /** O círculo de runas no chão se fecha conforme a canalização avança. */
    private static void circle(ServerPlayerEntity player, Channel c) {
        ServerWorld world = player.getLevel();
        float progress = 1.0F - c.left / (float) c.total;
        int points = 3;
        for (int i = 0; i < points; i++) {
            double angle = 2.0D * Math.PI * (progress + i / (double) points * 0.02D);
            double x = player.getX() + Math.cos(angle) * 1.4D, z = player.getZ() + Math.sin(angle) * 1.4D;
            world.sendParticles(ParticleTypes.ENCHANT, x, player.getY() + 0.1D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            world.sendParticles(ParticleTypes.END_ROD, x, player.getY() + 0.05D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        if (CHANNELS.remove(player.getUUID()) != null) {
            Msg.bar(player, TextFormatting.YELLOW, "mm_broken");
            player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundCategory.PLAYERS, 0.6F, 1.6F);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHANNELS.remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        CHANNELS.clear();
        READY_AT.clear();
    }
}
