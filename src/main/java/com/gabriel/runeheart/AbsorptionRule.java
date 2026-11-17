package com.gabriel.runeheart;

/** Contas da absorção, sem depender do Minecraft (por isso dá para testar). */
public final class AbsorptionRule {
    private AbsorptionRule() {
    }

    /** Resultado de um passo de absorção. */
    public static final class Step {
        public final float mana;      // mana que o jogador ganha
        public final float natural;   // mana natural que sai do chunk
        public final boolean overload;

        Step(float mana, float natural, boolean overload) {
            this.mana = mana;
            this.natural = natural;
            this.overload = overload;
        }
    }

    /**
     * Um passo de {@code seconds} segundos de absorção.
     *
     * @param density       densidade do lugar (0 a 100)
     * @param mana          mana atual do jogador
     * @param maxMana       mana máxima do jogador
     * @param percentAtFull mana por segundo com densidade 100, em % do máximo
     * @param naturalCost   mana natural gasta por segundo com a mana enchendo no ritmo cheio
     * @param overloadBelow abaixo dessa densidade, sobrecarga em vez de ganho
     */
    public static Step step(float density, float mana, float maxMana, float percentAtFull, float naturalCost,
                            int overloadBelow, float seconds) {
        if (density < overloadBelow) return new Step(0.0F, 0.0F, true);
        if (maxMana <= 0.0F || mana >= maxMana || seconds <= 0.0F) return new Step(0.0F, 0.0F, false);
        float share = ManaMath.clamp(density / NaturalManaRule.MAX, 0.0F, 1.0F);
        float wanted = maxMana * percentAtFull / 100.0F * share * seconds;
        float gained = Math.min(wanted, maxMana - mana);
        // a mana natural gasta acompanha o que de fato entrou (encher o último pedacinho custa menos)
        float natural = wanted <= 0.0F ? 0.0F : naturalCost * seconds * (gained / wanted);
        return new Step(gained, Math.min(natural, density), false);
    }

    /** A sintonia avança neste tick? (parado, Mana Sense ligado, lugar denso o bastante) */
    public static boolean attunes(boolean still, boolean sensing, float density, int minDensity) {
        return still && sensing && density >= minDensity;
    }
}
