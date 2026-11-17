package com.gabriel.runeheart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TextFormatting;

/**
 * {@code /runeheart mana}: mostra a mana natural de onde o jogador está, com as partes da conta.
 * {@code /runeheart mana drain <quanto>} e {@code /runeheart mana refill} (só operadores): para testar.
 */
public final class RuneCommand {
    private RuneCommand() {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal(RuneheartMod.MODID)
                .then(Commands.literal("mana")
                        .executes(c -> show(c.getSource().getPlayerOrException()))
                        .then(Commands.literal("drain").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F, 100.0F))
                                        .executes(c -> drain(c.getSource().getPlayerOrException(),
                                                FloatArgumentType.getFloat(c, "amount")))))
                        .then(Commands.literal("refill").requires(s -> s.hasPermission(2))
                                .executes(c -> refill(c.getSource().getPlayerOrException())))));
    }

    private static int show(ServerPlayerEntity p) {
        if (!RuneConfig.NM_ENABLED.get()) {
            Msg.chat(p, TextFormatting.GRAY, "nm_disabled");
            return 0;
        }
        NaturalMana.Reading r = NaturalMana.read(p.getLevel(), p.blockPosition());
        Msg.chat(p, TextFormatting.GREEN, "nm_now", Msg.f1(r.current), Msg.f1(Math.min(100.0F, r.capacity.total + r.storm)));
        Msg.chat(p, TextFormatting.GRAY, "nm_parts", Msg.tr(TextFormatting.GRAY, "biome_kind." + r.kind.name().toLowerCase()),
                Msg.f1(r.capacity.base), Msg.f1(r.capacity.vegetation), Msg.f1(r.capacity.water),
                Msg.f1(r.capacity.buildings), Msg.f1(r.storm), Msg.f1(r.deficit));
        return Math.round(r.current);
    }

    private static int drain(ServerPlayerEntity p, float amount) {
        float taken = NaturalMana.drain(p.getLevel(), p.blockPosition(), amount);
        Msg.chat(p, TextFormatting.YELLOW, "nm_drained", Msg.f1(taken));
        return Math.round(taken);
    }

    private static int refill(ServerPlayerEntity p) {
        NaturalMana.refill(p.getLevel(), p.blockPosition());
        Msg.chat(p, TextFormatting.YELLOW, "nm_refilled");
        return 1;
    }
}
