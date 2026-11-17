package com.gabriel.runeheart;

import com.gabriel.runeheart.NaturalManaRule.BiomeKind;
import com.gabriel.runeheart.NaturalManaRule.Capacity;
import com.gabriel.runeheart.NaturalManaRule.Survey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NaturalManaRuleTest {
    private static Capacity cap(BiomeKind kind, float veg, int flowing, int built) {
        return NaturalManaRule.capacity(kind, new Survey(veg, flowing, built, 256), 20.0F, 10.0F, 30.0F);
    }

    @Test
    void denseForestGetsTheFullVegetationBonus() {
        Capacity c = cap(BiomeKind.FOREST, 200.0F, 0, 0);       // mais de 60% das colunas
        assertEquals(20.0F, c.vegetation, 0.001F);
        assertEquals(75.0F, c.total, 0.001F);
    }

    @Test
    void halfCoveredGetsAPartialBonus() {
        Capacity c = cap(BiomeKind.PLAINS, 76.8F, 0, 0);        // 30% das colunas = metade do bônus
        assertEquals(10.0F, c.vegetation, 0.001F);
    }

    @Test
    void waterfallNeedsAFewColumns() {
        assertEquals(0.0F, cap(BiomeKind.FOREST, 0.0F, 2, 0).water, 0.001F);
        assertEquals(10.0F, cap(BiomeKind.FOREST, 0.0F, 3, 0).water, 0.001F);
    }

    @Test
    void citiesDryOut() {
        Capacity c = cap(BiomeKind.PLAINS, 0.0F, 0, 128);       // metade do chunk construída: perda inteira
        assertEquals(30.0F, c.buildings, 0.001F);
        assertEquals(8.0F, c.total, 0.001F);
        assertEquals(0.0F, cap(BiomeKind.DESERT, 0.0F, 0, 256).total, 0.001F);   // nunca abaixo de zero
    }

    @Test
    void neverAboveOneHundred() {
        assertEquals(100.0F, cap(BiomeKind.LUSH, 256.0F, 10, 0).total, 0.001F);
    }

    @Test
    void deficitRefillsLinearly() {
        // 100 em 6000 ticks: 50 de déficit somem em 3000 ticks
        assertEquals(25.0F, NaturalManaRule.deficitAfter(50.0F, 1500, 6000), 0.001F);
        assertEquals(0.0F, NaturalManaRule.deficitAfter(50.0F, 3000, 6000), 0.001F);
        assertEquals(0.0F, NaturalManaRule.deficitAfter(50.0F, 99999, 6000), 0.001F);
    }

    @Test
    void currentAddsTheStormAndTakesTheDeficit() {
        assertEquals(60.0F, NaturalManaRule.current(75.0F, 0.0F, 15.0F), 0.001F);
        assertEquals(100.0F, NaturalManaRule.current(95.0F, 15.0F, 0.0F), 0.001F);  // tempestade não passa de 100
        assertEquals(0.0F, NaturalManaRule.current(20.0F, 0.0F, 40.0F), 0.001F);
    }
}
