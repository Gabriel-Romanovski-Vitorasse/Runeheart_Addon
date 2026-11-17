package com.gabriel.runeheart;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente -> servidor, ações do Mana Method: salvar o arranjo de um slot, escolher um slot, trocar para o próximo,
 * ou conjurar o escolhido.
 */
public class RuneActionPacket {
    public static final int SAVE = 0, SELECT = 1, NEXT = 2, CAST = 3;

    final int action, slot;
    final String text;

    public RuneActionPacket(int action, int slot, String text) {
        this.action = action;
        this.slot = slot;
        this.text = text == null ? "" : text;
    }

    public static void encode(RuneActionPacket p, PacketBuffer buf) {
        buf.writeVarInt(p.action);
        buf.writeVarInt(p.slot);
        buf.writeUtf(p.text, 128);
    }

    public static RuneActionPacket decode(PacketBuffer buf) {
        return new RuneActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(128));
    }

    public static void handle(RuneActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayerEntity player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (player != null) ManaMethod.onAction(player, p.action, p.slot, p.text);
        });
        ctx.get().setPacketHandled(true);
    }
}
