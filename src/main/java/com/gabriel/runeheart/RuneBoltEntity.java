package com.gabriel.runeheart;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/** Projétil do Disparo: voa reto (sem gravidade), deixa um rastro de runas e some depois de 3 segundos. */
public class RuneBoltEntity extends ThrowableEntity {
    private static final int LIFETIME = 60;

    private float damage;
    private boolean area;

    public RuneBoltEntity(EntityType<? extends RuneBoltEntity> type, World world) {
        super(type, world);
    }

    public RuneBoltEntity(World world, LivingEntity owner, float damage, boolean area) {
        super(RuneheartMod.RUNE_BOLT.get(), owner, world);
        this.damage = damage;
        this.area = area;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            level.addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.ENCHANT, getX(), getY(), getZ(), random.nextGaussian() * 0.2D, 0.1D, random.nextGaussian() * 0.2D);
        } else if (tickCount > LIFETIME) {
            remove();
        }
    }

    @Override
    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (level.isClientSide) return;
        Entity owner = getOwner();
        LivingEntity direct = result instanceof EntityRayTraceResult && ((EntityRayTraceResult) result).getEntity() instanceof LivingEntity
                ? (LivingEntity) ((EntityRayTraceResult) result).getEntity() : null;
        if (direct == owner) return;                                    // não acerta quem disparou
        RuneEffects.burst((ServerWorld) level, owner instanceof PlayerEntity ? (PlayerEntity) owner : null,
                result.getLocation(), damage, area, direct);
        remove();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && e != getOwner();
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putFloat("damage", damage);
        nbt.putBoolean("area", area);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        damage = nbt.getFloat("damage");
        area = nbt.getBoolean("area");
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
