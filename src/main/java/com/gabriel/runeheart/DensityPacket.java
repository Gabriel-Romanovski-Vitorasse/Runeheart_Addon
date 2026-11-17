package com.gabriel.runeheart;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: a mana natural de onde o jogador está (agora e cheia) e se o Mana Sense dele está ligado. */
public class DensityPacket {
    final int density;
    final int capacity;
    final boolean sensing;

    public DensityPacket(int density, int capacity, boolean sensing) {
        this.density = density;
        this.capacity = capacity;
        this.sensing = sensing;
    }

    public static void encode(DensityPacket p, PacketBuffer buf) {
        buf.writeByte(p.density);
        buf.writeByte(p.capacity);
        buf.writeBoolean(p.sensing);
    }

    public static DensityPacket decode(PacketBuffer buf) {
        return new DensityPacket(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readBoolean());
    }

    public static void handle(DensityPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNaturalMana.receive(p.density, p.capacity, p.sensing)));
        ctx.get().setPacketHandled(true);
    }
}
