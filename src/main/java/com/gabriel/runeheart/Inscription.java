package com.gabriel.runeheart;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Inscrição de runas numa magia do Block Clover: a magia faz o papel da runa núcleo, e as runas inscritas a
 * melhoram (Potência, Ampliação, Múltiplo, Duração, Gatilho) ou somam um efeito ao acerto (Selo, Impulso,
 * Cura, Barreira). O Disparo não entra: quem dispara é a magia. Salva como texto: {@code "power+seal"}.
 * Sem depender do Minecraft (dá para testar).
 */
public final class Inscription {
    public final Set<Rune> runes;

    public Inscription(Set<Rune> runes) {
        this.runes = runes.isEmpty() ? Collections.<Rune>emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(runes));
    }

    public boolean has(Rune r) {
        return runes.contains(r);
    }

    public int size() {
        return runes.size();
    }

    public String encode() {
        StringBuilder b = new StringBuilder();
        for (Rune r : runes) {
            if (b.length() > 0) b.append('+');
            b.append(r.id);
        }
        return b.toString();
    }

    /** Lê uma inscrição salva. Vazia ou com erro devolve null (magia sem inscrição). */
    public static Inscription parse(String text) {
        if (text == null || text.isEmpty()) return null;
        Set<Rune> set = EnumSet.noneOf(Rune.class);
        for (String part : text.split("\\+")) {
            Rune r = Rune.byId(part);
            if (r == null || !set.add(r)) return null;
        }
        return new Inscription(set);
    }

    /** Por que a inscrição não vale (chave de mensagem), ou null se vale. */
    public String problem(int maxRunes, int knownMask) {
        if (runes.contains(Rune.BOLT)) return "insc_no_bolt";
        if (runes.size() > maxRunes) return "arr_too_many";
        for (Rune r : runes) if ((knownMask & r.bit()) == 0) return "arr_unknown_rune";
        return null;
    }

    /** Multiplicador do dano dos acertos: com Potência, 1 + (mult - 1) × força; sem ela, 1. */
    public static float damageMultiplier(boolean power, float strength, float powerMultiplier) {
        return power ? 1.0F + (powerMultiplier - 1.0F) * ManaMath.clamp(strength, 0.0F, 1.0F) : 1.0F;
    }

    /** Mana natural gasta por uma recarga da inscrição. */
    public float naturalCost(float perRune) {
        return perRune * runes.size();
    }

    /**
     * Pacote (do Block Clover) de uma classe de habilidade, ex.: {@code "lightning"} para
     * {@code com.yuanno.block_clover.spells.lightning.ThunderFiendAbility}. Null se não for uma magia.
     */
    public static String attributeOf(String abilityClassName) {
        String marker = ".spells.";
        int i = abilityClassName == null ? -1 : abilityClassName.indexOf(marker);
        if (i < 0) return null;
        int start = i + marker.length(), end = abilityClassName.indexOf('.', start);
        return end < 0 ? null : abilityClassName.substring(start, end);
    }

    /** Magias que aceitam inscrição: todas as de atributo, menos Anti-Magia e Tempo (e as técnicas gerais de "misc"). */
    public static boolean eligible(String abilityClassName) {
        String attr = attributeOf(abilityClassName);
        return attr != null && !attr.equals("antimagic") && !attr.equals("time") && !attr.equals("misc");
    }
}
