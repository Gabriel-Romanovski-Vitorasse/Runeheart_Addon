package com.gabriel.runeheart;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Efeitos das runas núcleo, com as modificadoras. {@code strength} vem da mana natural do lugar (densidade 100 = 1)
 * e já inclui a Potência. Os números base ficam na seção {@code mana_method} da config.
 */
public final class RuneEffects {
    private RuneEffects() {
    }

    private static double radius() {
        return RuneConfig.MM_AREA_RADIUS.get();
    }

    private static int ticks(double seconds, Arrangement a) {
        return RuneMath.ticks(seconds, a.has(Rune.DURATION), RuneConfig.MM_DURATION_MULT.get());
    }

    /**
     * Conjuração normal (sem Gatilho), a partir do jogador.
     *
     * @return false se não teve efeito (ex.: Selo sem alvo); a conjuração não conta para a maestria
     */
    static boolean cast(ServerPlayerEntity p, Arrangement a, float strength) {
        ServerWorld w = p.getLevel();
        switch (a.core) {
            case BOLT:
                for (float angle : RuneMath.spread(a.has(Rune.MULTIPLE))) {
                    RuneBoltEntity bolt = new RuneBoltEntity(w, p, strength * RuneConfig.f(RuneConfig.MM_BOLT_DAMAGE),
                            a.has(Rune.AMPLIFY));
                    bolt.shootFromRotation(p, p.xRot, p.yRot + angle, 0.0F, RuneConfig.f(RuneConfig.MM_BOLT_SPEED), 0.0F);
                    w.addFreshEntity(bolt);
                }
                sound(p, SoundEvents.EVOKER_CAST_SPELL, 1.4F);
                return true;
            case BARRIER:
                for (LivingEntity t : allies(p, a)) barrier(t, a, strength);
                sound(p, SoundEvents.BEACON_ACTIVATE, 1.5F);
                return true;
            case HEAL:
                for (LivingEntity t : allies(p, a)) heal(t, a, strength);
                sound(p, SoundEvents.BREWING_STAND_BREW, 1.6F);
                return true;
            case SEAL: {
                LivingEntity target = targetInSight(p, RuneConfig.MM_RANGE.get());
                if (target == null) {
                    Msg.bar(p, TextFormatting.GRAY, "mm_no_target");
                    return false;
                }
                seal(p, target, a, strength);
                return true;
            }
            case IMPULSE:
                impulse(p, a, strength);
                return true;
            default:
                return false;
        }
    }

    /** Efeito de uma armadilha em quem pisou nela ({@code caster} pode ser null se o dono saiu). */
    static void trap(ServerWorld w, PlayerEntity caster, LivingEntity target, Arrangement a, float strength) {
        switch (a.core) {
            case BOLT: {
                float dmg = strength * RuneConfig.f(RuneConfig.MM_BOLT_DAMAGE);
                burst(w, caster, target.position(), dmg, a.has(Rune.AMPLIFY), target);
                break;
            }
            case BARRIER: barrier(target, a, strength); break;
            case HEAL: heal(target, a, strength); break;
            case SEAL: seal(caster, target, a, strength); break;
            case IMPULSE:
                target.setDeltaMovement(target.getDeltaMovement().add(0.0D,
                        0.6D + RuneConfig.MM_IMPULSE.get() * strength * 0.6D, 0.0D));
                target.hurtMarked = true;
                break;
            default: break;
        }
    }

    // ---------- efeitos ----------

    private static void barrier(LivingEntity t, Arrangement a, float strength) {
        int dur = ticks(RuneConfig.MM_BARRIER_SECONDS.get(), a);
        float points = RuneConfig.f(RuneConfig.MM_BARRIER_ABSORPTION) * strength;
        int amp = Math.max(0, Math.round(points / 4.0F) - 1);                    // cada nível de Absorção = 4 pontos
        t.addEffect(new EffectInstance(Effects.ABSORPTION, dur, amp, false, true, true));
        t.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, dur, 0, false, true, true));
        ring(t, ParticleTypes.END_ROD, 1.0D, 24);
    }

    private static void heal(LivingEntity t, Arrangement a, float strength) {
        t.heal(RuneConfig.f(RuneConfig.MM_HEAL) * strength);
        if (a.has(Rune.DURATION)) t.addEffect(new EffectInstance(Effects.REGENERATION, ticks(5.0D, a), 0, false, true, true));
        ring(t, ParticleTypes.HAPPY_VILLAGER, 0.8D, 16);
    }

    private static void seal(PlayerEntity caster, LivingEntity target, Arrangement a, float strength) {
        int dur = Math.max(20, Math.round(ticks(RuneConfig.MM_SEAL_SECONDS.get(), a) * Math.min(1.0F, Math.max(0.25F, strength))));
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        if (a.has(Rune.AMPLIFY)) {
            for (LivingEntity e : target.level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius()))) {
                if (e != target && e != caster && e.isAlive()) targets.add(e);
            }
        }
        for (LivingEntity e : targets) {
            e.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, dur, 4, false, true, true));
            e.addEffect(new EffectInstance(Effects.WEAKNESS, dur, 1, false, true, true));
            ring(e, ParticleTypes.ENCHANT, 1.0D, 30);
        }
        target.level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
    }

    private static void impulse(ServerPlayerEntity p, Arrangement a, float strength) {
        double force = RuneConfig.MM_IMPULSE.get() * strength;
        boolean pull = p.isShiftKeyDown();
        Vector3d look = p.getViewVector(1.0F);
        double reach = a.has(Rune.AMPLIFY) ? radius() : 6.0D;
        for (Entity e : p.level.getEntities(p, p.getBoundingBox().inflate(reach))) {
            if (!(e instanceof LivingEntity) || !e.isAlive()) continue;
            Vector3d to = e.position().subtract(p.position());
            if (to.length() > reach || to.length() < 0.01D) continue;
            if (!a.has(Rune.AMPLIFY) && to.normalize().dot(look) < 0.5D) continue;   // sem Ampliação: só em frente
            Vector3d push = to.normalize().scale(pull ? -force : force).add(0.0D, pull ? 0.1D : 0.35D * strength, 0.0D);
            e.setDeltaMovement(e.getDeltaMovement().add(push));
            e.hurtMarked = true;
        }
        ServerWorld w = p.getLevel();
        w.sendParticles(ParticleTypes.CLOUD, p.getX() + look.x * 1.5D, p.getY() + 1.0D, p.getZ() + look.z * 1.5D,
                16, 0.5D, 0.4D, 0.5D, 0.05D);
        sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, pull ? 0.8F : 1.3F);
    }

    /** Explosão do Disparo (no impacto com Ampliação, ou na armadilha). Não quebra blocos. */
    static void burst(ServerWorld w, PlayerEntity caster, Vector3d at, float damage, boolean area, LivingEntity direct) {
        DamageSource src = caster == null ? DamageSource.MAGIC : DamageSource.indirectMagic(caster, caster);
        if (direct != null) direct.hurt(src, damage);
        if (area) {
            AxisAlignedBB box = new AxisAlignedBB(at, at).inflate(radius() * 0.5D);
            for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == caster || e == direct || !e.isAlive()) continue;
                e.hurt(src, damage * 0.6F);
            }
            w.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 0.5D, at.z, 60, radius() * 0.25D, 0.4D, radius() * 0.25D, 0.6D);
        }
        w.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5D, at.z, 14, 0.3D, 0.3D, 0.3D, 0.08D);
        w.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.9F, 1.3F);
    }

    // ---------- ajudantes ----------

    /** Quem a Barreira e a Cura alcançam: o próprio jogador, e com Ampliação os jogadores em volta. */
    private static List<LivingEntity> allies(ServerPlayerEntity p, Arrangement a) {
        List<LivingEntity> list = new ArrayList<>();
        list.add(p);
        if (a.has(Rune.AMPLIFY)) {
            for (PlayerEntity other : p.level.getEntitiesOfClass(PlayerEntity.class, p.getBoundingBox().inflate(radius()))) {
                if (other != p && other.isAlive() && !other.isSpectator()) list.add(other);
            }
        }
        return list;
    }

    /** Entidade viva na mira, até {@code reach} blocos, sem atravessar blocos (ou null). */
    static LivingEntity targetInSight(PlayerEntity player, double reach) {
        Vector3d eye = player.getEyePosition(1.0F);
        Vector3d end = eye.add(player.getViewVector(1.0F).scale(reach));
        BlockRayTraceResult wall = player.level.clip(new RayTraceContext(eye, end, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, player));
        if (wall.getType() != RayTraceResult.Type.MISS) end = wall.getLocation();
        AxisAlignedBB box = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityRayTraceResult hit = ProjectileHelper.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, eye.distanceToSqr(end));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private static void ring(LivingEntity t, IParticleData type, double r, int count) {
        if (!(t.level instanceof ServerWorld)) return;
        ServerWorld w = (ServerWorld) t.level;
        for (int i = 0; i < count; i++) {
            double ang = 2.0D * Math.PI * i / count;
            w.sendParticles(type, t.getX() + Math.cos(ang) * r, t.getY() + 0.2D, t.getZ() + Math.sin(ang) * r, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
    }

    private static void sound(PlayerEntity p, net.minecraft.util.SoundEvent s, float pitch) {
        p.level.playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, 0.9F, pitch);
    }
}
