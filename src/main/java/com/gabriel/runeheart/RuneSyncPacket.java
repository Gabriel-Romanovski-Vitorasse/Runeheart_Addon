package com.gabriel.runeheart;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente: runas conhecidas, arranjos, slot escolhido e maestria do pr\u00f3prio jogador, e as magias do
 * Block Clover equipadas que aceitam inscri\u00e7\u00e3o (chave, nome e runas inscritas).
 */
public class RuneSyncPacket {
    private static final int MAX_SPELLS = 64;

    final int known, selected, maxRunes, slots, casts;
    final String[] texts;
    final String[] spellKeys, spellNames, spellInscriptions;

    public RuneSyncPacket(int known, String[] texts, int selected, int maxRunes, int slots, int casts,
                          String[] spellKeys, String[] spellNames, String[] spellInscriptions) {
        this.known = known;
        this.texts = texts;
        this.selected = selected;
        this.maxRunes = maxRunes;
        this.slots = slots;
        this.casts = casts;
        this.spellKeys = spellKeys;
        this.spellNames = spellNames;
        this.spellInscriptions = spellInscriptions;
    }

    public static void encode(RuneSyncPacket p, PacketBuffer buf) {
        buf.writeVarInt(p.known);
        buf.writeVarInt(p.selected);
        buf.writeVarInt(p.maxRunes);
        buf.writeVarInt(p.slots);
        buf.writeVarInt(p.casts);
        buf.writeVarInt(p.texts.length);
        for (String t : p.texts) buf.writeUtf(t, 128);
        int n = Math.min(MAX_SPELLS, p.spellKeys.length);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeUtf(p.spellKeys[i], 128);
            buf.writeUtf(p.spellNames[i], 128);
            buf.writeUtf(p.spellInscriptions[i], 128);
        }
    }

    public static RuneSyncPacket decode(PacketBuffer buf) {
        int known = buf.readVarInt(), selected = buf.readVarInt(), maxRunes = buf.readVarInt(), slots = buf.readVarInt(),
                casts = buf.readVarInt();
        String[] texts = new String[Math.min(RuneData.MAX_SLOTS, buf.readVarInt())];
        for (int i = 0; i < texts.length; i++) texts[i] = buf.readUtf(128);
        int n = Math.min(MAX_SPELLS, buf.readVarInt());
        String[] keys = new String[n], names = new String[n], insc = new String[n];
        for (int i = 0; i < n; i++) {
            keys[i] = buf.readUtf(128);
            names[i] = buf.readUtf(128);
            insc[i] = buf.readUtf(128);
        }
        return new RuneSyncPacket(known, texts, selected, maxRunes, slots, casts, keys, names, insc);
    }

    public static void handle(RuneSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRunes.receive(p)));
        ctx.get().setPacketHandled(true);
    }
}
