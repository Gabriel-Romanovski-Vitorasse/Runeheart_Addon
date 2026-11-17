package com.gabriel.runeheart;

/** Contas dos efeitos das runas, sem depender do Minecraft (por isso dá para testar). */
public final class RuneMath {
    private RuneMath() {
    }

    /** Força do arranjo: densidade/100, vezes o multiplicador da Potência se ela estiver no arranjo. */
    public static float strength(float density, boolean power, float powerMultiplier) {
        float base = ManaMath.clamp(density / NaturalManaRule.MAX, 0.0F, 1.0F);
        return power ? base * powerMultiplier : base;
    }

    /** Tempo em ticks, vezes o multiplicador da Duração se ela estiver no arranjo. */
    public static int ticks(double seconds, boolean duration, double durationMultiplier) {
        return (int) Math.round(seconds * 20.0D * (duration ? durationMultiplier : 1.0D));
    }

    /** Ângulos (em graus, em volta da mira) dos projéteis do Disparo: 1 sem Múltiplo, 3 em leque com ele. */
    public static float[] spread(boolean multiple) {
        return multiple ? new float[] { -12.0F, 0.0F, 12.0F } : new float[] { 0.0F };
    }
}
