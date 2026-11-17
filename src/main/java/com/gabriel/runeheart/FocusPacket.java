package com.gabriel.runeheart;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: a tecla de concentração foi apertada ou solta. */
public class FocusPacket {
    final boolean down;

    public FocusPacket(boolean down) {
        this.down = down;
    }

    public static void encode(FocusPacket p, PacketBuffer buf) {
        buf.writeBoolean(p.down);
    }

    public static FocusPacket decode(PacketBuffer buf) {
        return new FocusPacket(buf.readBoolean());
    }

    public static void handle(FocusPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player == null) return;
            if (p.down) Absorption.begin(player); else Absorption.end(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
