package com.gabriel.runeheart;

/** Contas pequenas de mana, sem depender do Minecraft (por isso dá para testar). */
public final class ManaMath {
    private ManaMath() {
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }
}
