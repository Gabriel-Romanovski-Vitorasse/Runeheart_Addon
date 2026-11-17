package com.gabriel.runeheart;

/**
 * As runas do Mana Method. Um arranjo tem uma runa núcleo (o que a magia faz) e runas modificadoras (como faz).
 * O {@link #id} é o nome salvo no jogador e usado nos arquivos de idioma ({@code rune.runeheart.<id>}).
 */
public enum Rune {
    BOLT("bolt", true), BARRIER("barrier", true), SEAL("seal", true), HEAL("heal", true), IMPULSE("impulse", true),
    AMPLIFY("amplify", false), DURATION("duration", false), POWER("power", false), TRIGGER("trigger", false),
    MULTIPLE("multiple", false);

    public final String id;
    public final boolean core;

    Rune(String id, boolean core) {
        this.id = id;
        this.core = core;
    }

    public static Rune byId(String id) {
        for (Rune r : values()) if (r.id.equals(id)) return r;
        return null;
    }

    /** Bit desta runa numa máscara (para mandar o conjunto de runas conhecidas num número só). */
    public int bit() {
        return 1 << ordinal();
    }

    public String langKey() {
        return "rune." + RuneheartMod.MODID + "." + id;
    }
}
