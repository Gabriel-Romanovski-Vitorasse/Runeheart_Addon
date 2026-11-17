package com.gabriel.runeheart;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: runas conhecidas, arranjos, slot escolhido e maestria do próprio jogador. */
public class RuneSyncPacket {
    final int known, selected, maxRunes, slots, casts;
    final String[] texts;

    public RuneSyncPacket(int known, String[] texts, int selected, int maxRunes, int slots, int casts) {
        this.known = known;
        this.texts = texts;
        this.selected = selected;
        this.maxRunes = maxRunes;
        this.slots = slots;
        this.casts = casts;
    }

    public static void encode(RuneSyncPacket p, PacketBuffer buf) {
        buf.writeVarInt(p.known);
        buf.writeVarInt(p.selected);
        buf.writeVarInt(p.maxRunes);
        buf.writeVarInt(p.slots);
        buf.writeVarInt(p.casts);
        buf.writeVarInt(p.texts.length);
        for (String t : p.texts) buf.writeUtf(t, 128);
    }

    public static RuneSyncPacket decode(PacketBuffer buf) {
        int known = buf.readVarInt(), selected = buf.readVarInt(), maxRunes = buf.readVarInt(), slots = buf.readVarInt(),
                casts = buf.readVarInt();
        String[] texts = new String[Math.min(RuneData.MAX_SLOTS, buf.readVarInt())];
        for (int i = 0; i < texts.length; i++) texts[i] = buf.readUtf(128);
        return new RuneSyncPacket(known, texts, selected, maxRunes, slots, casts);
    }

    public static void handle(RuneSyncPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRunes.receive(p)));
        ctx.get().setPacketHandled(true);
    }
}
