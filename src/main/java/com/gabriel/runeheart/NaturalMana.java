package com.gabriel.runeheart;

import com.gabriel.runeheart.NaturalManaRule.BiomeKind;
import com.gabriel.runeheart.NaturalManaRule.Capacity;
import com.gabriel.runeheart.NaturalManaRule.Survey;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Mana natural no servidor: quanto cada chunk tem agora, quanto ele tem quando cheio, e puxar mana dele.
 * <p>
 * A capacidade de um chunk vem do bioma e do terreno (vegetação, água corrente, construções). O terreno é
 * contado quando o chunk é usado e de novo a cada {@code natural_mana.rescan_seconds}; o que foi puxado fica em
 * {@link NaturalManaData} e volta sozinho com o tempo.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NaturalMana {
    private static final Logger LOG = LogManager.getLogger();
    /** De quanto em quanto tempo cada jogador recebe a densidade do lugar onde está, em ticks. */
    static final int SEND_EVERY = 10;

    private static final class Scan {
        final BiomeKind kind;
        final Capacity capacity;
        final long at;

        Scan(BiomeKind kind, Capacity capacity, long at) {
            this.kind = kind;
            this.capacity = capacity;
            this.at = at;
        }
    }

    /** Terreno contado, por dimensão e chunk (só na memória: é recontado ao abrir o mundo). */
    private static final Map<RegistryKey<World>, Map<Long, Scan>> SCANS = new HashMap<>();

    private NaturalMana() {
    }

    /** Estado da mana natural num ponto, para o comando e o HUD. */
    public static final class Reading {
        public final BiomeKind kind;
        public final Capacity capacity;
        public final float storm, deficit, current;

        Reading(BiomeKind kind, Capacity capacity, float storm, float deficit) {
            this.kind = kind;
            this.capacity = capacity;
            this.storm = storm;
            this.deficit = deficit;
            this.current = NaturalManaRule.current(capacity.total, storm, deficit);
        }
    }

    private static int refillTicks() {
        return RuneConfig.NM_REGEN_SECONDS.get() * 20;
    }

    private static NaturalManaData data(ServerWorld world) {
        return world.getDataStorage().computeIfAbsent(NaturalManaData::new, NaturalManaData.NAME);
    }

    /** Mana natural no ponto agora. */
    public static Reading read(ServerWorld world, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        Scan scan = scan(world, chunk);
        float storm = world.isThundering() && scan.kind != BiomeKind.NETHER && scan.kind != BiomeKind.END
                ? RuneConfig.f(RuneConfig.NM_STORM_BONUS) : 0.0F;
        float deficit = data(world).deficit(chunk.toLong(), world.getGameTime(), refillTicks());
        return new Reading(scan.kind, scan.capacity, storm, deficit);
    }

    /** Densidade no ponto agora (0 a 100); 0 com a mana natural desligada. */
    public static float density(ServerWorld world, BlockPos pos) {
        if (!RuneConfig.NM_ENABLED.get()) return 0.0F;
        return read(world, pos).current;
    }

    /**
     * Puxa até {@code amount} de mana natural do chunk do ponto.
     *
     * @return quanto realmente saiu (menos, se o chunk não tinha tudo)
     */
    public static float drain(ServerWorld world, BlockPos pos, float amount) {
        if (!RuneConfig.NM_ENABLED.get() || amount <= 0.0F) return 0.0F;
        Reading r = read(world, pos);
        float taken = Math.min(amount, r.current);
        if (taken <= 0.0F) return 0.0F;
        data(world).setDeficit(new ChunkPos(pos).toLong(), r.deficit + taken, world.getGameTime());
        return taken;
    }

    /** Devolve o chunk ao estado cheio (comando de teste). */
    public static void refill(ServerWorld world, BlockPos pos) {
        data(world).setDeficit(new ChunkPos(pos).toLong(), 0.0F, world.getGameTime());
    }

    // ---------- Terreno ----------

    private static Scan scan(ServerWorld world, ChunkPos chunk) {
        Map<Long, Scan> byChunk = SCANS.computeIfAbsent(world.dimension(), k -> new HashMap<>());
        long now = world.getGameTime();
        Scan old = byChunk.get(chunk.toLong());
        if (old != null && now - old.at < RuneConfig.NM_RESCAN_SECONDS.get() * 20L) return old;
        BlockPos center = new BlockPos(chunk.getMinBlockX() + 8, 64, chunk.getMinBlockZ() + 8);
        BiomeKind kind = kindOf(world, world.getBiome(center));
        Survey survey = world.hasChunk(chunk.x, chunk.z) ? survey(world, chunk) : new Survey(0, 0, 0, 1);
        Capacity cap = NaturalManaRule.capacity(kind, survey, RuneConfig.f(RuneConfig.NM_VEGETATION_BONUS),
                RuneConfig.f(RuneConfig.NM_WATER_BONUS), RuneConfig.f(RuneConfig.NM_BUILDING_PENALTY));
        Scan scan = new Scan(kind, cap, now);
        byChunk.put(chunk.toLong(), scan);
        if (RuneConfig.DEBUG_LOG.get()) {
            LOG.info("[Runeheart] chunk {} contado: {} base {}, vegetação {}/{}, água corrente {}, construções {} -> {}",
                    chunk, kind, kind.base, survey.vegetation, survey.columns, survey.flowing, survey.built, cap.total);
        }
        return scan;
    }

    static BiomeKind kindOf(ServerWorld world, Biome biome) {
        ResourceLocation id = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(biome);
        String path = id == null ? "" : id.getPath();
        if (path.contains("flower_forest") || path.contains("dark_forest")) return BiomeKind.LUSH;
        switch (biome.getBiomeCategory()) {
            case JUNGLE: return BiomeKind.LUSH;
            case FOREST:
            case TAIGA: return BiomeKind.FOREST;
            case SWAMP: return BiomeKind.SWAMP;
            case RIVER: return BiomeKind.RIVER;
            case EXTREME_HILLS: return BiomeKind.HILLS;
            case PLAINS: return BiomeKind.PLAINS;
            case SAVANNA: return BiomeKind.SAVANNA;
            case MUSHROOM: return BiomeKind.MUSHROOM;
            case BEACH: return BiomeKind.BEACH;
            case ICY: return BiomeKind.ICY;
            case OCEAN: return BiomeKind.OCEAN;
            case MESA: return BiomeKind.MESA;
            case DESERT: return BiomeKind.DESERT;
            case NETHER: return BiomeKind.NETHER;
            case THEEND: return BiomeKind.END;
            default: return BiomeKind.OTHER;
        }
    }

    /** Conta as 256 colunas do chunk, olhando a superfície e um pouco abaixo dela. */
    private static Survey survey(ServerWorld world, ChunkPos chunk) {
        float vegetation = 0.0F;
        int flowing = 0, built = 0;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunk.getMinBlockX() + dx, z = chunk.getMinBlockZ() + dz;
                int top = world.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;   // folhas e água contam aqui
                float veg = 0.0F;
                boolean water = false, building = false;
                for (int y = top + 1; y >= Math.max(0, top - 6); y--) {
                    BlockState s = world.getBlockState(p.set(x, y, z));
                    veg = Math.max(veg, plantValue(s));
                    if (isBuilt(s)) building = true;
                    FluidState f = s.getFluidState();
                    if (y >= top - 1 && !f.isEmpty() && f.is(FluidTags.WATER) && !f.isSource()) water = true;
                }
                vegetation += veg;
                if (water) flowing++;
                if (building) built++;
            }
        }
        return new Survey(vegetation, flowing, built, 256);
    }

    /** Quanto um bloco vale como vegetação: árvore e flor valem 1, plantas baixas menos, grama pura bem pouco. */
    static float plantValue(BlockState s) {
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) return 1.0F;
        if (s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.VINE) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.LILY_PAD)
                || s.is(Blocks.BAMBOO) || s.is(Blocks.SUGAR_CANE) || s.is(BlockTags.CROPS)) return 0.6F;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM)) return 0.25F;
        return 0.0F;
    }

    /** Bloco que só aparece na superfície por construção. */
    static boolean isBuilt(BlockState s) {
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.STONE_BRICKS) || s.is(Tags.Blocks.GLASS)
                || s.is(Tags.Blocks.GLASS_PANES) || s.is(Blocks.BRICKS) || s.is(Blocks.IRON_BLOCK)
                || s.is(Blocks.IRON_BARS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.SLABS)
                || s.is(BlockTags.STAIRS) || s.is(BlockTags.WALLS) || s.is(BlockTags.FENCES)
                || s.is(BlockTags.DOORS) || s.is(BlockTags.BEDS) || s.is(BlockTags.RAILS) || s.is(BlockTags.WOOL)
                || s.is(BlockTags.CARPETS) || s.is(Blocks.REDSTONE_WIRE) || s.is(Blocks.REDSTONE_BLOCK)
                || s.is(Blocks.REPEATER) || s.is(Blocks.COMPARATOR) || s.is(Blocks.PISTON)
                || s.is(Blocks.STICKY_PISTON) || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.CHISELED_STONE_BRICKS)
                || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LANTERN);
    }

    // ---------- Envio para os clientes ----------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        if (player.tickCount % SEND_EVERY != 0) return;
        if (!RuneConfig.NM_ENABLED.get()) return;
        Reading r = read(player.getLevel(), player.blockPosition());
        RuneheartMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DensityPacket(Math.round(r.current), Math.round(Math.min(NaturalManaRule.MAX, r.capacity.total + r.storm)),
                        BlockCloverBridge.isManaSenseActive(player)));
    }

    /** Quando alguém sai, esquece os chunks daquela dimensão que já encheram (o arquivo não cresce à toa). */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer().level instanceof ServerWorld) {
            ServerWorld w = (ServerWorld) event.getPlayer().level;
            data(w).prune(w.getGameTime(), refillTicks());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        SCANS.clear();
    }
}
