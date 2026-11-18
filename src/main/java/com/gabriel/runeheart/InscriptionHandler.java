package com.gabriel.runeheart;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inscrição de runas nas magias do Block Clover. Não mexe no Block Clover: só reage ao que ele faz.
 * <ul>
 *   <li>Dano de magia ({@code AbilityDamageSource} diz qual magia foi): Potência aumenta o dano; Ampliação espalha
 *   parte dele em volta; Selo, Impulso, Cura e Barreira somam o efeito ao acerto.</li>
 *   <li>Projétil de magia surgindo no mundo: Duração aumenta o alcance; Múltiplo cria cópias em leque.</li>
 *   <li>Projétil batendo num bloco: com Gatilho, deixa uma armadilha que repete o golpe em quem pisar.</li>
 * </ul>
 * A força vem da mana natural do lugar e é medida (e cobrada) no máximo uma vez a cada {@code recharge_ticks}.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InscriptionHandler {
    static final String TAG_CLONE = RuneheartMod.MODID + "_clone";

    private static final class Charge {
        final float strength;
        final long tick;

        Charge(float strength, long tick) {
            this.strength = strength;
            this.tick = tick;
        }
    }

    private static final class PendingClone {
        final Entity original;
        final LivingEntity owner;
        final float angle;

        PendingClone(Entity original, LivingEntity owner, float angle) {
            this.original = original;
            this.owner = owner;
            this.angle = angle;
        }
    }

    private static final Map<UUID, Map<String, Charge>> CHARGES = new HashMap<>();
    private static final Map<UUID, Long> ON_HIT_READY = new HashMap<>();
    private static final List<PendingClone> CLONES = new ArrayList<>();

    private InscriptionHandler() {
    }

    private static Inscription inscriptionOf(ServerPlayerEntity p, BlockCloverBridge.Spell spell) {
        if (spell == null || !RuneConfig.INSC_ENABLED.get()) return null;
        Inscription i = RuneData.inscription(p, spell.key);
        if (i == null || i.problem(RuneData.maxRunes(p), RuneData.known(p)) != null) return null;
        return i;
    }

    /** Força da magia inscrita agora (0 = apagada por falta de mana natural). Cobra a mana natural se recarregou. */
    static float charge(ServerPlayerEntity p, BlockCloverBridge.Spell spell, Inscription insc) {
        long now = p.level.getGameTime();
        Map<String, Charge> mine = CHARGES.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        Charge c = mine.get(spell.key);
        if (c != null && now - c.tick < RuneConfig.INSC_RECHARGE.get()) return c.strength;

        ServerWorld w = p.getLevel();
        float density = NaturalMana.density(w, p.blockPosition());
        float strength;
        if (density < RuneConfig.MM_FIZZLE_BELOW.get()) {
            strength = 0.0F;
            Msg.bar(p, TextFormatting.RED, "insc_fizzle", spell.name, Math.round(density));
        } else {
            NaturalMana.drain(w, p.blockPosition(), insc.naturalCost(RuneConfig.f(RuneConfig.MM_NATURAL_COST)));
            strength = RuneMath.strength(density, false, 1.0F);
            if (RuneConfig.INSC_MASTERY.get()) countCast(p);
        }
        mine.put(spell.key, new Charge(strength, now));
        return strength;
    }

    private static void countCast(ServerPlayerEntity p) {
        int before = RuneData.casts(p);
        RuneData.setCasts(p, before + 1);
        if (Mastery.levelsUp(before, RuneConfig.MM_MASTERY_THREE.get(), RuneConfig.MM_MASTERY_FIVE.get())) {
            Msg.chat(p, TextFormatting.GOLD, "mastery_up", RuneData.maxRunes(p), RuneData.slots(p));
            RuneData.sync(p);
        }
    }

    private static int ticks(double seconds, Inscription i) {
        return RuneMath.ticks(seconds, i.has(Rune.DURATION), RuneConfig.MM_DURATION_MULT.get());
    }

    // ---------- Acertos ----------

    /** LOW: depois dos outros mods ajustarem o dano, antes da armadura (o Block Clover aplica o dano antes deste evento). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().level.isClientSide) return;
        DamageSource src = event.getSource();
        BlockCloverBridge.Spell spell = BlockCloverBridge.spellOfDamage(src);
        if (spell == null) return;
        ServerPlayerEntity p = playerBehind(src);
        LivingEntity target = event.getEntityLiving();
        if (p == null || target == p) return;
        Inscription insc = inscriptionOf(p, spell);
        if (insc == null) return;
        float strength = charge(p, spell, insc);
        if (strength <= 0.0F) return;
        float amount = event.getAmount() * Inscription.damageMultiplier(insc.has(Rune.POWER), strength,
                RuneConfig.f(RuneConfig.MM_POWER_MULT));
        event.setAmount(amount);
        hitEffects(p, target, amount, insc, strength);
    }

    /** Quem lançou: a entidade da fonte, ou o dono do projétil. */
    private static ServerPlayerEntity playerBehind(DamageSource src) {
        Entity e = src.getEntity();
        if (e instanceof ServerPlayerEntity) return (ServerPlayerEntity) e;
        Entity direct = src.getDirectEntity();
        if (direct instanceof ProjectileEntity && ((ProjectileEntity) direct).getOwner() instanceof ServerPlayerEntity) {
            return (ServerPlayerEntity) ((ProjectileEntity) direct).getOwner();
        }
        return null;
    }

    /** Efeitos das runas num acerto (também usados pela armadilha do Gatilho). */
    static void hitEffects(ServerPlayerEntity p, LivingEntity target, float amount, Inscription insc, float strength) {
        ServerWorld w = (ServerWorld) target.level;
        if (insc.has(Rune.AMPLIFY) && amount > 0.0F) {
            float splash = amount * RuneConfig.f(RuneConfig.INSC_SPLASH);
            double r = RuneConfig.MM_AREA_RADIUS.get() * 0.5D;
            DamageSource s = p == null ? DamageSource.MAGIC : DamageSource.indirectMagic(p, p);
            for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r))) {
                if (e == target || e == p || !e.isAlive()) continue;
                e.hurt(s, splash);
            }
            w.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 0.5D, target.getZ(), 40, r * 0.5D, 0.4D, r * 0.5D, 0.6D);
        }
        if (insc.has(Rune.SEAL)) {
            int dur = Math.max(20, Math.round(ticks(RuneConfig.MM_SEAL_SECONDS.get(), insc) * Math.max(0.25F, Math.min(1.0F, strength))));
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, dur, 4, false, true, true));
            target.addEffect(new EffectInstance(Effects.WEAKNESS, dur, 1, false, true, true));
            w.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 0.2D, target.getZ(), 20, 0.6D, 0.1D, 0.6D, 0.0D);
        }
        if (insc.has(Rune.IMPULSE) && p != null) {
            Vector3d away = target.position().subtract(p.position());
            if (away.lengthSqr() > 1.0E-4D) {
                double force = RuneConfig.MM_IMPULSE.get() * strength;
                target.setDeltaMovement(target.getDeltaMovement().add(away.normalize().scale(force).add(0.0D, 0.3D * strength, 0.0D)));
                target.hurtMarked = true;
            }
        }
        if (p != null && (insc.has(Rune.HEAL) || insc.has(Rune.BARRIER))) {
            long now = w.getGameTime();
            Long ready = ON_HIT_READY.get(p.getUUID());
            if (ready == null || now >= ready) {
                ON_HIT_READY.put(p.getUUID(), now + 20);
                float frac = RuneConfig.f(RuneConfig.INSC_ON_HIT);
                if (insc.has(Rune.HEAL)) {
                    p.heal(RuneConfig.f(RuneConfig.MM_HEAL) * strength * frac);
                    w.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0D, p.getZ(), 6, 0.4D, 0.5D, 0.4D, 0.0D);
                }
                if (insc.has(Rune.BARRIER)) {
                    float points = RuneConfig.f(RuneConfig.MM_BARRIER_ABSORPTION) * strength * frac;
                    p.setAbsorptionAmount(Math.min(p.getAbsorptionAmount() + points,
                            RuneConfig.f(RuneConfig.MM_BARRIER_ABSORPTION) * 2.0F));
                }
            }
        }
    }

    // ---------- Projéteis ----------

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide) return;
        Entity e = event.getEntity();
        if (!BlockCloverBridge.isSpellProjectile(e) || e.getPersistentData().getBoolean(TAG_CLONE)) return;
        Entity owner = ((ProjectileEntity) e).getOwner();
        if (!(owner instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) owner;
        BlockCloverBridge.Spell spell = BlockCloverBridge.spellOfProjectile(e);
        Inscription insc = inscriptionOf(p, spell);
        if (insc == null) return;
        float strength = charge(p, spell, insc);
        if (strength <= 0.0F) return;
        if (insc.has(Rune.DURATION)) BlockCloverBridge.scaleProjectileLife(e, RuneConfig.MM_DURATION_MULT.get());
        if (insc.has(Rune.MULTIPLE)) {
            int n = RuneConfig.INSC_MULTIPLE.get();
            for (int i = 0; i < n; i++) {
                int side = i % 2 == 0 ? 1 : -1;
                float angle = side * 12.0F * (i / 2 + 1);
                CLONES.add(new PendingClone(e, p, angle));                     // no fim do tick: não dá para criar entidades aqui
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CLONES.isEmpty()) return;
        List<PendingClone> todo = new ArrayList<>(CLONES);
        CLONES.clear();
        for (PendingClone c : todo) {
            Entity o = c.original;
            if (!o.isAlive() || !(o.level instanceof ServerWorld)) continue;
            Entity copy = o.getType().create(o.level);
            if (copy == null) continue;
            CompoundNBT tag = o.saveWithoutId(new CompoundNBT());
            tag.remove("UUID");
            copy.load(tag);
            copy.setUUID(UUID.randomUUID());
            copy.getPersistentData().putBoolean(TAG_CLONE, true);
            double rad = Math.toRadians(c.angle), cos = Math.cos(rad), sin = Math.sin(rad);
            Vector3d v = o.getDeltaMovement();
            copy.setDeltaMovement(new Vector3d(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos));
            copy.yRot = o.yRot - c.angle;
            if (copy instanceof ProjectileEntity) ((ProjectileEntity) copy).setOwner(c.owner);
            BlockCloverBridge.copyProjectileLinks(o, copy, c.owner);
            o.level.addFreshEntity(copy);
        }
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Entity e = event.getEntity();
        if (e.level.isClientSide || event.getRayTraceResult().getType() != RayTraceResult.Type.BLOCK) return;
        if (!BlockCloverBridge.isSpellProjectile(e)) return;
        Entity owner = ((ProjectileEntity) e).getOwner();
        if (!(owner instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) owner;
        BlockCloverBridge.Spell spell = BlockCloverBridge.spellOfProjectile(e);
        Inscription insc = inscriptionOf(p, spell);
        if (insc == null || !insc.has(Rune.TRIGGER)) return;
        float strength = charge(p, spell, insc);
        if (strength <= 0.0F) return;
        BlockRayTraceResult hit = (BlockRayTraceResult) event.getRayTraceResult();
        Vector3d at = Vector3d.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
        float damage = BlockCloverBridge.projectileDamage(e) * Inscription.damageMultiplier(insc.has(Rune.POWER), strength,
                RuneConfig.f(RuneConfig.MM_POWER_MULT));
        RuneTraps.placeInscribed(p, at, spell.name, insc, strength, damage);
    }

    /** A armadilha de uma magia inscrita disparou em {@code target}. */
    static void trapHit(ServerWorld w, PlayerEntity owner, LivingEntity target, Inscription insc, float strength, float damage) {
        DamageSource src = owner == null ? DamageSource.MAGIC : DamageSource.indirectMagic(owner, owner);
        if (damage > 0.0F) target.hurt(src, damage);
        hitEffects(owner instanceof ServerPlayerEntity ? (ServerPlayerEntity) owner : null, target, damage, insc, strength);
        w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.5F, 1.6F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHARGES.remove(event.getPlayer().getUUID());
        ON_HIT_READY.remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        CHARGES.clear();
        ON_HIT_READY.clear();
        CLONES.clear();
    }
}
