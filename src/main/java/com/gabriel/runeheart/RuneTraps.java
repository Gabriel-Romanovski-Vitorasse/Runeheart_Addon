package com.gabriel.runeheart;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Armadilhas da runa Gatilho: o arranjo fica num círculo no chão onde o jogador mirou e dispara em quem pisar
 * (menos o dono). Cada jogador tem no máximo {@code traps_per_player}; a mais antiga some quando passa disso.
 * Ficam só na memória (somem se o servidor fechar).
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RuneTraps {
    private static final class Trap {
        final RegistryKey<World> dim;
        final Vector3d at;
        final UUID owner;
        final Arrangement inner;
        final float strength;
        final long expires;

        Trap(RegistryKey<World> dim, Vector3d at, UUID owner, Arrangement inner, float strength, long expires) {
            this.dim = dim;
            this.at = at;
            this.owner = owner;
            this.inner = inner;
            this.strength = strength;
            this.expires = expires;
        }
    }

    private static final List<Trap> TRAPS = new ArrayList<>();

    private RuneTraps() {
    }

    /** Coloca a armadilha no bloco que o jogador está olhando. */
    static boolean place(ServerPlayerEntity p, Arrangement a, float strength) {
        Vector3d eye = p.getEyePosition(1.0F);
        Vector3d end = eye.add(p.getViewVector(1.0F).scale(RuneConfig.MM_RANGE.get()));
        BlockRayTraceResult hit = p.level.clip(new RayTraceContext(eye, end, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, p));
        if (hit.getType() == RayTraceResult.Type.MISS) {
            Msg.bar(p, TextFormatting.GRAY, "mm_no_ground");
            return false;
        }
        Vector3d at = Vector3d.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
        int lifetime = RuneMath.ticks(RuneConfig.MM_TRAP_SECONDS.get(), a.has(Rune.DURATION), RuneConfig.MM_DURATION_MULT.get());
        // limite por jogador: tira a mais antiga dele
        List<Trap> mine = new ArrayList<>();
        for (Trap t : TRAPS) if (t.owner.equals(p.getUUID())) mine.add(t);
        while (mine.size() >= RuneConfig.MM_TRAPS_PER_PLAYER.get()) TRAPS.remove(mine.remove(0));
        TRAPS.add(new Trap(p.level.dimension(), at, p.getUUID(), a.without(Rune.TRIGGER), strength,
                p.level.getGameTime() + lifetime));
        p.level.playSound(null, at.x, at.y, at.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8F, 0.7F);
        Msg.bar(p, TextFormatting.AQUA, "mm_trap_set", Msg.f1(lifetime / 20.0D));
        return true;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof ServerWorld) || TRAPS.isEmpty()) return;
        ServerWorld w = (ServerWorld) event.world;
        long now = w.getGameTime();
        for (Iterator<Trap> it = TRAPS.iterator(); it.hasNext(); ) {
            Trap t = it.next();
            if (t.dim != w.dimension()) continue;
            if (now >= t.expires) { it.remove(); continue; }
            if (!w.isLoaded(new net.minecraft.util.math.BlockPos(t.at))) continue;
            if (now % 10 == 0) {                                              // círculo discreto no chão
                for (int i = 0; i < 8; i++) {
                    double ang = 2.0D * Math.PI * i / 8 + now * 0.05D;
                    w.sendParticles(ParticleTypes.ENCHANT, t.at.x + Math.cos(ang) * 0.9D, t.at.y + 0.05D,
                            t.at.z + Math.sin(ang) * 0.9D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            if (now % 2 != 0) continue;
            AxisAlignedBB box = new AxisAlignedBB(t.at.x - 1.0D, t.at.y - 0.2D, t.at.z - 1.0D, t.at.x + 1.0D, t.at.y + 1.5D, t.at.z + 1.0D);
            for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!e.isAlive() || e.getUUID().equals(t.owner) || e.isSpectator()) continue;
                PlayerEntity owner = w.getServer().getPlayerList().getPlayer(t.owner);
                RuneEffects.trap(w, owner, e, t.inner, t.strength);
                w.sendParticles(ParticleTypes.END_ROD, t.at.x, t.at.y + 0.2D, t.at.z, 20, 0.6D, 0.1D, 0.6D, 0.1D);
                w.playSound(null, t.at.x, t.at.y, t.at.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundCategory.PLAYERS, 0.8F, 1.4F);
                it.remove();
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        TRAPS.clear();
    }
}
