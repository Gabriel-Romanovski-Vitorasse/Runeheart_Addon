package com.gabriel.runeheart;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.loot.ConstantRange;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.ItemLootEntry;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.loot.functions.SetNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pergaminhos de runa nos baús de estruturas. A chance ({@code mana_method.scroll_chest_chance}) é lida na hora
 * em que o baú é gerado, então mudar a config vale sem recarregar nada. Runas básicas (Disparo, Barreira,
 * Ampliação) saem mais.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RuneLoot {
    private static final Set<String> CHESTS = new HashSet<>(Arrays.asList(
            "chests/simple_dungeon", "chests/abandoned_mineshaft", "chests/desert_pyramid", "chests/jungle_temple",
            "chests/stronghold_library", "chests/stronghold_corridor", "chests/woodland_mansion",
            "chests/pillager_outpost", "chests/buried_treasure", "chests/igloo_chest", "chests/shipwreck_treasure",
            "chests/underwater_ruin_big", "chests/ruined_portal"));

    static LootConditionType CHANCE_TYPE;

    private RuneLoot() {
    }

    /** Registra a condição de chance (no setup comum). */
    static void registerCondition() {
        if (CHANCE_TYPE == null) {
            CHANCE_TYPE = Registry.register(Registry.LOOT_CONDITION_TYPE, new ResourceLocation(RuneheartMod.MODID, "scroll_chance"),
                    new LootConditionType(new ChanceSerializer()));
        }
    }

    /** "Sorteia pela chance da config." */
    static final class Chance implements ILootCondition {
        @Override
        public LootConditionType getType() {
            return CHANCE_TYPE;
        }

        @Override
        public boolean test(LootContext ctx) {
            return RuneConfig.MM_ENABLED.get() && ctx.getRandom().nextDouble() < RuneConfig.MM_SCROLL_CHANCE.get();
        }
    }

    static final class ChanceSerializer implements ILootSerializer<Chance> {
        @Override
        public void serialize(JsonObject json, Chance value, JsonSerializationContext ctx) {
        }

        @Override
        public Chance deserialize(JsonObject json, JsonDeserializationContext ctx) {
            return new Chance();
        }
    }

    static int weight(Rune r) {
        return r == Rune.BOLT || r == Rune.BARRIER || r == Rune.AMPLIFY ? 14 : 8;
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!"minecraft".equals(name.getNamespace()) || !CHESTS.contains(name.getPath())) return;
        LootPool.Builder pool = LootPool.lootPool().name(RuneheartMod.MODID + "_rune_scrolls")
                .setRolls(ConstantRange.exactly(1)).when(Chance::new);
        for (Rune r : Rune.values()) {
            CompoundNBT tag = new CompoundNBT();
            tag.putString("rune", r.id);
            pool.add(ItemLootEntry.lootTableItem(RuneheartMod.RUNE_SCROLL.get()).setWeight(weight(r)).apply(SetNBT.setTag(tag)));
        }
        event.getTable().addPool(pool.build());
    }
}
