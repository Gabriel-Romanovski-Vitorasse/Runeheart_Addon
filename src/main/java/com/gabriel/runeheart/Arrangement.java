package com.gabriel.runeheart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Um arranjo do Mana Method: uma runa núcleo e até algumas modificadoras, sem repetir.
 * Salvo como texto, o núcleo primeiro: {@code "bolt+power+multiple"}. Sem depender do Minecraft (dá para testar).
 */
public final class Arrangement {
    /** Tempo de canalização por runa, em ticks (0,5 s). */
    public static final int TICKS_PER_RUNE = 10;

    public final Rune core;
    public final Set<Rune> modifiers;

    public Arrangement(Rune core, Set<Rune> modifiers) {
        this.core = core;
        this.modifiers = modifiers.isEmpty() ? Collections.<Rune>emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
    }

    public int size() {
        return 1 + modifiers.size();
    }

    public boolean has(Rune r) {
        return core == r || modifiers.contains(r);
    }

    public int channelTicks() {
        return size() * TICKS_PER_RUNE;
    }

    /** Todas as runas, o núcleo primeiro. */
    public List<Rune> runes() {
        List<Rune> all = new ArrayList<>();
        all.add(core);
        all.addAll(modifiers);
        return all;
    }

    /** O mesmo arranjo sem uma modificadora (ex.: o efeito que a armadilha guarda, sem o Gatilho). */
    public Arrangement without(Rune modifier) {
        Set<Rune> mods = modifiers.isEmpty() ? EnumSet.noneOf(Rune.class) : EnumSet.copyOf(modifiers);
        mods.remove(modifier);
        return new Arrangement(core, mods);
    }

    public String encode() {
        StringBuilder b = new StringBuilder(core.id);
        for (Rune m : modifiers) b.append('+').append(m.id);
        return b.toString();
    }

    /** Lê um arranjo salvo. Texto vazio ou com erro devolve null (slot vazio). */
    public static Arrangement parse(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] parts = text.split("\\+");
        Rune core = Rune.byId(parts[0]);
        if (core == null || !core.core) return null;
        Set<Rune> mods = EnumSet.noneOf(Rune.class);
        for (int i = 1; i < parts.length; i++) {
            Rune m = Rune.byId(parts[i]);
            if (m == null || m.core || !mods.add(m)) return null;
        }
        return new Arrangement(core, mods);
    }

    /** Por que o arranjo não vale (chave de mensagem), ou null se vale. */
    public String problem(int maxRunes, int knownMask) {
        if (size() > maxRunes) return "arr_too_many";
        for (Rune r : runes()) if ((knownMask & r.bit()) == 0) return "arr_unknown_rune";
        if (modifiers.contains(Rune.MULTIPLE) && core != Rune.BOLT) return "arr_multiple_needs_bolt";
        return null;
    }
}
