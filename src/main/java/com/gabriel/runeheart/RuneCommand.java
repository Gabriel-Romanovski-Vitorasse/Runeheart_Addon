package com.gabriel.runeheart;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.ISuggestionProvider;
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
                                .executes(c -> refill(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("attune")
                        .executes(c -> attuneStatus(c.getSource().getPlayerOrException()))
                        .then(Commands.literal("grant").requires(s -> s.hasPermission(2))
                                .executes(c -> attuneSet(c.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .executes(c -> attuneSet(c.getSource().getPlayerOrException(), false))))
                .then(Commands.literal("runes")
                        .executes(c -> runes(c.getSource().getPlayerOrException()))
                        .then(Commands.literal("grant").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("rune", StringArgumentType.word())
                                        .suggests((c, b) -> ISuggestionProvider.suggest(runeIds(), b))
                                        .executes(c -> grant(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "rune")))))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2))
                                .executes(c -> resetRunes(c.getSource().getPlayerOrException())))
                        .then(Commands.literal("casts").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                        .executes(c -> setCasts(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "count")))))));
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

    private static int attuneStatus(ServerPlayerEntity p) {
        if (Absorption.isAttuned(p)) {
            Msg.chat(p, TextFormatting.GREEN, "attune_status_done");
            return 1;
        }
        Msg.chat(p, TextFormatting.GRAY, "attune_status", p.getPersistentData().getInt(Absorption.TAG_ATTUNE_SECONDS),
                RuneConfig.ABS_ATTUNEMENT_SECONDS.get(), RuneConfig.ABS_ATTUNEMENT_DENSITY.get());
        return 0;
    }

    private static int attuneSet(ServerPlayerEntity p, boolean attuned) {
        p.getPersistentData().putBoolean(Absorption.TAG_ATTUNED, attuned);
        p.getPersistentData().putInt(Absorption.TAG_ATTUNE_SECONDS, 0);
        Msg.chat(p, TextFormatting.YELLOW, attuned ? "attune_granted" : "attune_reset");
        return 1;
    }

    private static java.util.List<String> runeIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        ids.add("all");
        for (Rune r : Rune.values()) ids.add(r.id);
        return ids;
    }

    private static int runes(ServerPlayerEntity p) {
        int count = Integer.bitCount(RuneData.known(p));
        Msg.chat(p, TextFormatting.AQUA, "runes_status", count, Rune.values().length, RuneData.casts(p),
                RuneData.maxRunes(p), RuneData.slots(p));
        for (Rune r : Rune.values()) {
            if (RuneData.knows(p, r)) Msg.chat(p, TextFormatting.GRAY, "runes_line", new net.minecraft.util.text.TranslationTextComponent(r.langKey()));
        }
        return count;
    }

    private static int grant(ServerPlayerEntity p, String id) {
        if ("all".equals(id)) {
            for (Rune r : Rune.values()) RuneData.learn(p, r);
        } else {
            Rune r = Rune.byId(id);
            if (r == null) {
                Msg.chat(p, TextFormatting.RED, "runes_unknown_id", id);
                return 0;
            }
            RuneData.learn(p, r);
        }
        RuneData.sync(p);
        Msg.chat(p, TextFormatting.YELLOW, "runes_granted", id);
        return 1;
    }

    private static int resetRunes(ServerPlayerEntity p) {
        RuneData.setKnown(p, 0);
        RuneData.setCasts(p, 0);
        for (int i = 0; i < RuneData.MAX_SLOTS; i++) RuneData.setSlot(p, i, null);
        RuneData.sync(p);
        Msg.chat(p, TextFormatting.YELLOW, "runes_reset");
        return 1;
    }

    private static int setCasts(ServerPlayerEntity p, int casts) {
        RuneData.setCasts(p, casts);
        RuneData.sync(p);
        Msg.chat(p, TextFormatting.YELLOW, "runes_casts_set", casts, RuneData.maxRunes(p), RuneData.slots(p));
        return casts;
    }

    private static int refill(ServerPlayerEntity p) {
        NaturalMana.refill(p.getLevel(), p.blockPosition());
        Msg.chat(p, TextFormatting.YELLOW, "nm_refilled");
        return 1;
    }
}
