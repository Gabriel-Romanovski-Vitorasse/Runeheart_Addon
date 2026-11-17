package com.gabriel.runeheart;

/**
 * Contas da mana natural, sem depender do Minecraft (por isso dá para testar).
 * <p>
 * A densidade de um chunk vai de 0 a 100: a base do bioma, mais a vegetação e a água corrente, menos as
 * construções. O que foi puxado do chunk fica guardado como "déficit", que volta a zero com o tempo.
 */
public final class NaturalManaRule {
    public static final float MAX = 100.0F;
    /** Fração das colunas com vegetação que já dá o bônus inteiro (uma floresta não cobre 100% do chão). */
    static final float FULL_VEGETATION = 0.6F;
    /** Fração das colunas com construções que já tira a perda inteira. */
    static final float FULL_BUILDINGS = 0.4F;
    /** Colunas com água corrente para contar como "tem água corrente" (algumas poucas já são uma cachoeira). */
    static final int MIN_FLOWING = 3;

    private NaturalManaRule() {
    }

    /** Densidade base de cada tipo de bioma. */
    public enum BiomeKind {
        LUSH(75), FOREST(55), SWAMP(50), RIVER(45), HILLS(40), PLAINS(38), SAVANNA(35), MUSHROOM(60),
        BEACH(25), ICY(25), OCEAN(20), MESA(15), DESERT(10), NETHER(5), END(0), OTHER(30);

        public final float base;

        BiomeKind(float base) {
            this.base = base;
        }
    }

    /** O que foi contado no terreno de um chunk. */
    public static final class Survey {
        public final float vegetation;   // colunas com vegetação (pode ter frações: grama vale menos que uma árvore)
        public final int flowing;        // colunas com água corrente
        public final int built;          // colunas com construções
        public final int columns;        // colunas examinadas

        public Survey(float vegetation, int flowing, int built, int columns) {
            this.vegetation = vegetation;
            this.flowing = flowing;
            this.built = built;
            this.columns = columns;
        }
    }

    /** Capacidade (densidade cheia) e suas partes, para mostrar no comando. */
    public static final class Capacity {
        public final float base, vegetation, water, buildings, total;

        Capacity(float base, float vegetation, float water, float buildings) {
            this.base = base;
            this.vegetation = vegetation;
            this.water = water;
            this.buildings = buildings;
            this.total = ManaMath.clamp(base + vegetation + water - buildings, 0.0F, MAX);
        }
    }

    public static Capacity capacity(BiomeKind kind, Survey s, float vegetationBonus, float waterBonus, float buildingPenalty) {
        float cols = Math.max(1, s.columns);
        float veg = vegetationBonus * Math.min(1.0F, s.vegetation / cols / FULL_VEGETATION);
        float water = s.flowing >= MIN_FLOWING ? waterBonus : 0.0F;
        float built = buildingPenalty * Math.min(1.0F, s.built / cols / FULL_BUILDINGS);
        return new Capacity(kind.base, veg, water, built);
    }

    /** Quanto o déficit diminuiu depois de {@code elapsedTicks}, enchendo de 0 a 100 em {@code refillTicks}. */
    public static float deficitAfter(float deficit, long elapsedTicks, int refillTicks) {
        if (deficit <= 0.0F) return 0.0F;
        if (refillTicks <= 0) return 0.0F;
        return Math.max(0.0F, deficit - elapsedTicks * (MAX / refillTicks));
    }

    /** Densidade atual: a capacidade (com a tempestade) menos o déficit, entre 0 e 100. */
    public static float current(float capacity, float stormBonus, float deficit) {
        return ManaMath.clamp(Math.min(MAX, capacity + stormBonus) - deficit, 0.0F, MAX);
    }
}
