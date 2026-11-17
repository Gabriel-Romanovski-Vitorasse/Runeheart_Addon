package com.gabriel.runeheart;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * O que o jogador sabe do Mana Method, salvo nos dados dele: runas conhecidas, arranjos dos slots, slot escolhido e
 * conjurações feitas (maestria). Tudo fica num compound só, copiado ao morrer.
 */
@Mod.EventBusSubscriber(modid = RuneheartMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RuneData {
    static final String TAG = RuneheartMod.MODID + "_runes";
    public static final int MAX_SLOTS = 5;

    private RuneData() {
    }

    private static CompoundNBT root(PlayerEntity p) {
        CompoundNBT data = p.getPersistentData();
        if (!data.contains(TAG, 10)) data.put(TAG, new CompoundNBT());
        return data.getCompound(TAG);
    }

    public static int known(PlayerEntity p) {
        return root(p).getInt("known");
    }

    public static boolean knows(PlayerEntity p, Rune r) {
        return (known(p) & r.bit()) != 0;
    }

    /** @return true se a runa era nova */
    public static boolean learn(PlayerEntity p, Rune r) {
        int k = known(p);
        if ((k & r.bit()) != 0) return false;
        root(p).putInt("known", k | r.bit());
        return true;
    }

    public static void setKnown(PlayerEntity p, int mask) {
        root(p).putInt("known", mask);
    }

    public static int casts(PlayerEntity p) {
        return root(p).getInt("casts");
    }

    public static void setCasts(PlayerEntity p, int casts) {
        root(p).putInt("casts", Math.max(0, casts));
    }

    public static int maxRunes(PlayerEntity p) {
        return Mastery.maxRunes(casts(p), RuneConfig.MM_MASTERY_THREE.get(), RuneConfig.MM_MASTERY_FIVE.get());
    }

    public static int slots(PlayerEntity p) {
        return Mastery.slots(casts(p), RuneConfig.MM_MASTERY_FIVE.get());
    }

    public static String slotText(PlayerEntity p, int slot) {
        return root(p).getString("slot" + slot);
    }

    public static Arrangement slot(PlayerEntity p, int slot) {
        return Arrangement.parse(slotText(p, slot));
    }

    public static void setSlot(PlayerEntity p, int slot, Arrangement a) {
        root(p).putString("slot" + slot, a == null ? "" : a.encode());
    }

    public static int selected(PlayerEntity p) {
        return Math.min(root(p).getInt("selected"), slots(p) - 1);
    }

    public static void select(PlayerEntity p, int slot) {
        root(p).putInt("selected", Math.floorMod(slot, slots(p)));
    }

    // ---------- inscri\u00e7\u00f5es (runas nas magias do Block Clover) ----------

    private static CompoundNBT inscriptions(PlayerEntity p) {
        CompoundNBT r = root(p);
        if (!r.contains("insc", 10)) r.put("insc", new CompoundNBT());
        return r.getCompound("insc");
    }

    public static Inscription inscription(PlayerEntity p, String spellKey) {
        return Inscription.parse(inscriptions(p).getString(spellKey));
    }

    public static void setInscription(PlayerEntity p, String spellKey, Inscription i) {
        if (i == null || i.size() == 0) inscriptions(p).remove(spellKey);
        else inscriptions(p).putString(spellKey, i.encode());
    }

    /** Manda ao cliente o estado atual (para a tela de arranjos e o aviso do slot escolhido). */
    public static void sync(ServerPlayerEntity p) {
        String[] texts = new String[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) texts[i] = slotText(p, i);
        java.util.List<BlockCloverBridge.Spell> spells = BlockCloverBridge.equippedSpells(p);
        String[] keys = new String[spells.size()], names = new String[spells.size()], insc = new String[spells.size()];
        for (int i = 0; i < spells.size(); i++) {
            keys[i] = spells.get(i).key;
            names[i] = spells.get(i).name;
            insc[i] = inscriptions(p).getString(keys[i]);
        }
        RuneheartMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new RuneSyncPacket(known(p), texts, selected(p), maxRunes(p), slots(p), casts(p), keys, names, insc));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundNBT from = event.getOriginal().getPersistentData();
        if (from.contains(TAG, 10)) event.getPlayer().getPersistentData().put(TAG, from.getCompound(TAG).copy());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) sync((ServerPlayerEntity) event.getPlayer());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) sync((ServerPlayerEntity) event.getPlayer());
    }
}
