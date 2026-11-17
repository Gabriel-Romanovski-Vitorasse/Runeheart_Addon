package com.gabriel.runeheart;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
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
import java.util.Random;
import java.util.UUID;

/**
 * Absorção: segurando a tecla de concentração, parado, o jogador puxa a mana natural do lugar para a própria mana.
 * Levar dano ou se mexer quebra a concentração (é preciso soltar e apertar de novo). Num lugar seco demais, em vez
 * de mana vem a sobrecarga: náusea e dano.
 * <p>
 * Antes, o jogador precisa da sintonia ("sentir a mana natural"): ficar parado com o Mana Sense do Block Clover
 * ligado num lugar denso, somando {@code absorption.attunement_seconds}.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Absorption {
    static final String TAG_ATTUNED = RuneheartMod.MODID + "_attuned";
    static final String TAG_ATTUNE_SECONDS = RuneheartMod.MODID + "_attune_seconds";
    /** A cada quantos ticks a absorção rende (e gasta). */
    static final int STEP_TICKS = 5;

    private static final class Session {
        boolean broken;
        Vector3d last;
        int ticks;

        Session(Vector3d at) {
            this.last = at;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Posição de cada jogador no último segundo, para a sintonia saber se ele ficou parado. */
    private static final Map<UUID, Vector3d> LAST_SECOND = new HashMap<>();
    private static final Random RANDOM = new Random();
    /** true enquanto o próprio addon causa o dano da sobrecarga (ele não quebra a concentração). */
    private static boolean overloading;

    private Absorption() {
    }

    public static boolean isAttuned(PlayerEntity player) {
        return !RuneConfig.ABS_REQUIRE_ATTUNEMENT.get() || player.getPersistentData().getBoolean(TAG_ATTUNED);
    }

    static void begin(ServerPlayerEntity player) {
        Session s = new Session(player.position());
        SESSIONS.put(player.getUUID(), s);
        if (!RuneConfig.ABS_ENABLED.get() || !RuneConfig.NM_ENABLED.get()) {
            s.broken = true;
            Msg.bar(player, TextFormatting.GRAY, "abs_disabled");
        } else if (BlockCloverBridge.getMaxMana(player) <= 0.0F) {
            s.broken = true;
            Msg.bar(player, TextFormatting.GRAY, "abs_no_mana_stats");
        } else if (!isAttuned(player)) {
            s.broken = true;
            boolean sensing = BlockCloverBridge.isManaSenseActive(player);
            float density = NaturalMana.density(player.getLevel(), player.blockPosition());
            Msg.bar(player, TextFormatting.GRAY, "abs_not_attuned", RuneConfig.ABS_ATTUNEMENT_DENSITY.get(),
                    Msg.tr(sensing ? TextFormatting.GREEN : TextFormatting.RED, sensing ? "sense_on" : "sense_off"),
                    Math.round(density), player.getPersistentData().getInt(TAG_ATTUNE_SECONDS),
                    RuneConfig.ABS_ATTUNEMENT_SECONDS.get());
        }
    }

    static void end(ServerPlayerEntity player) {
        SESSIONS.remove(player.getUUID());
    }

    private static void breakFocus(ServerPlayerEntity player, Session s, String reason) {
        if (s.broken) return;
        s.broken = true;
        Msg.bar(player, TextFormatting.YELLOW, reason);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        if (player.tickCount % 20 == 0) attune(player);

        Session s = SESSIONS.get(player.getUUID());
        if (s == null || s.broken) return;
        if (!player.isAlive()) { SESSIONS.remove(player.getUUID()); return; }

        Vector3d now = player.position();
        double moved = Math.sqrt(Math.pow(now.x - s.last.x, 2) + Math.pow(now.z - s.last.z, 2));
        s.last = now;
        if (moved > RuneConfig.ABS_MOVE_TOLERANCE.get()) {
            breakFocus(player, s, "abs_broken_move");
            return;
        }
        s.ticks++;
        if (s.ticks % STEP_TICKS != 0) return;

        ServerWorld world = player.getLevel();
        float density = NaturalMana.density(world, player.blockPosition());
        float mana = BlockCloverBridge.getMana(player), max = BlockCloverBridge.getMaxMana(player);
        AbsorptionRule.Step step = AbsorptionRule.step(density, mana, max, RuneConfig.f(RuneConfig.ABS_MANA_PERCENT),
                RuneConfig.f(RuneConfig.ABS_NATURAL_COST), RuneConfig.ABS_OVERLOAD_BELOW.get(), STEP_TICKS / 20.0F);

        if (step.overload) {
            overload(player, s, density);
            return;
        }
        if (step.mana <= 0.0F) {
            Msg.bar(player, TextFormatting.AQUA, "abs_full");
            return;
        }
        BlockCloverBridge.addMana(player, step.mana);
        BlockCloverBridge.syncToClient(player);
        NaturalMana.drain(world, player.blockPosition(), step.natural);
        Msg.bar(player, TextFormatting.GREEN, "abs_active", Msg.f1(step.mana * 20.0F / STEP_TICKS), Math.round(density));
        effects(player, world, density);
    }

    private static void overload(ServerPlayerEntity player, Session s, float density) {
        Msg.bar(player, TextFormatting.RED, "abs_overload", Math.round(density));
        if (s.ticks % 40 != 0) return;
        player.addEffect(new EffectInstance(Effects.CONFUSION, 120, 0, false, false, true));
        float dmg = RuneConfig.f(RuneConfig.ABS_OVERLOAD_DAMAGE);
        if (dmg > 0.0F) {
            overloading = true;
            try {
                player.hurt(DamageSource.MAGIC, dmg);
            } finally {
                overloading = false;
            }
        }
    }

    /** Runas da mesa de encantamento voando até o jogador, pólen no chão e um zumbido baixo. */
    private static void effects(ServerPlayerEntity player, ServerWorld world, float density) {
        double cx = player.getX(), cy = player.getY() + 1.0D, cz = player.getZ();
        int count = 2 + Math.round(density / 25.0F);
        for (int i = 0; i < count; i++) {
            double dx = (RANDOM.nextDouble() - 0.5D) * 5.0D, dy = -0.6D - RANDOM.nextDouble(), dz = (RANDOM.nextDouble() - 0.5D) * 5.0D;
            // com count 0 o último valor é a velocidade: a partícula de encantamento nasce em (x+dx...) e voa até (x, y, z)
            world.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, 0, dx, dy, dz, 1.0D);
        }
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, player.getY() + 0.1D, cz, 2, 1.2D, 0.05D, 1.2D, 0.0D);
        if (player.tickCount % 40 == 0) {
            world.playSound(null, cx, cy, cz, SoundEvents.BEACON_AMBIENT, SoundCategory.PLAYERS, 0.35F, 1.6F);
        }
    }

    /** Sintonia: a cada segundo parado, com o Mana Sense ligado, num lugar denso, soma 1 segundo. */
    private static void attune(ServerPlayerEntity player) {
        Vector3d now = player.position();
        Vector3d before = LAST_SECOND.put(player.getUUID(), now);
        if (!RuneConfig.ABS_ENABLED.get() || !RuneConfig.ABS_REQUIRE_ATTUNEMENT.get() || before == null) return;
        CompoundNBT data = player.getPersistentData();
        if (data.getBoolean(TAG_ATTUNED)) return;
        boolean still = now.distanceTo(before) <= 0.3D;
        boolean sensing = BlockCloverBridge.isManaSenseActive(player);
        if (!still || !sensing) return;                          // só quando ele está tentando: sem mensagem
        float density = NaturalMana.density(player.getLevel(), player.blockPosition());
        int need = RuneConfig.ABS_ATTUNEMENT_SECONDS.get();
        if (!AbsorptionRule.attunes(true, true, density, RuneConfig.ABS_ATTUNEMENT_DENSITY.get())) {
            Msg.bar(player, TextFormatting.GRAY, "attune_too_dry", Math.round(density), RuneConfig.ABS_ATTUNEMENT_DENSITY.get());
            return;
        }
        int seconds = data.getInt(TAG_ATTUNE_SECONDS) + 1;
        data.putInt(TAG_ATTUNE_SECONDS, seconds);
        if (seconds < need) {
            Msg.bar(player, TextFormatting.GREEN, "attune_progress", seconds, need);
            return;
        }
        data.putBoolean(TAG_ATTUNED, true);
        player.level.playSound(null, player.getX(), player.getY() + 1.0D, player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 1.4F);
        player.getLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(),
                30, 0.8D, 0.8D, 0.8D, 0.0D);
        Msg.chat(player, TextFormatting.GREEN, "attune_done");
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (overloading || !(event.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        Session s = SESSIONS.get(player.getUUID());
        if (s != null) breakFocus(player, s, "abs_broken_hurt");
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundNBT from = event.getOriginal().getPersistentData(), to = event.getPlayer().getPersistentData();
        to.putBoolean(TAG_ATTUNED, from.getBoolean(TAG_ATTUNED));
        to.putInt(TAG_ATTUNE_SECONDS, from.getInt(TAG_ATTUNE_SECONDS));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getPlayer().getUUID());
        LAST_SECOND.remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        SESSIONS.clear();
        LAST_SECOND.clear();
    }
}
