package com.gabriel.runeheart;

/** Maestria do Mana Method: quantas runas cabem num arranjo e quantos slots o jogador tem, pelas conjurações feitas. */
public final class Mastery {
    private Mastery() {
    }

    /** Runas por arranjo: 2, depois 3 e por fim 5. */
    public static int maxRunes(int casts, int toThree, int toFive) {
        return casts >= toFive ? 5 : (casts >= toThree ? 3 : 2);
    }

    /** Slots de arranjo: 3, e 5 na maestria máxima. */
    public static int slots(int casts, int toFive) {
        return casts >= toFive ? 5 : 3;
    }

    /** Esta conjuração fez o jogador subir de nível? (para avisar uma vez só) */
    public static boolean levelsUp(int castsBefore, int toThree, int toFive) {
        return maxRunes(castsBefore, toThree, toFive) != maxRunes(castsBefore + 1, toThree, toFive);
    }
}
