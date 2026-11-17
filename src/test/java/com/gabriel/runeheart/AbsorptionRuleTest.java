package com.gabriel.runeheart;

import com.gabriel.runeheart.AbsorptionRule.Step;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsorptionRuleTest {
    @Test
    void fullDensityGivesTheFullRate() {
        Step s = AbsorptionRule.step(100.0F, 0.0F, 200.0F, 3.0F, 2.0F, 10, 1.0F);   // 3% de 200 = 6 por segundo
        assertEquals(6.0F, s.mana, 0.001F);
        assertEquals(2.0F, s.natural, 0.001F);
        assertFalse(s.overload);
    }

    @Test
    void rateFollowsTheDensity() {
        assertEquals(3.0F, AbsorptionRule.step(50.0F, 0.0F, 200.0F, 3.0F, 2.0F, 10, 1.0F).mana, 0.001F);
    }

    @Test
    void stopsAtMaxManaAndCostsLessForTheLastBit() {
        Step s = AbsorptionRule.step(100.0F, 197.0F, 200.0F, 3.0F, 2.0F, 10, 1.0F);   // faltavam 3 de 6
        assertEquals(3.0F, s.mana, 0.001F);
        assertEquals(1.0F, s.natural, 0.001F);
        Step full = AbsorptionRule.step(100.0F, 200.0F, 200.0F, 3.0F, 2.0F, 10, 1.0F);
        assertEquals(0.0F, full.mana, 0.001F);
        assertEquals(0.0F, full.natural, 0.001F);
    }

    @Test
    void dryPlaceOverloads() {
        Step s = AbsorptionRule.step(9.0F, 0.0F, 200.0F, 3.0F, 2.0F, 10, 1.0F);
        assertTrue(s.overload);
        assertEquals(0.0F, s.mana, 0.001F);
    }

    @Test
    void neverDrawsMoreThanThePlaceHas() {
        Step s = AbsorptionRule.step(10.5F, 0.0F, 200.0F, 3.0F, 20.0F, 10, 1.0F);
        assertEquals(10.5F, s.natural, 0.001F);
    }

    @Test
    void attunementNeedsAllThree() {
        assertTrue(AbsorptionRule.attunes(true, true, 60.0F, 60));
        assertFalse(AbsorptionRule.attunes(false, true, 80.0F, 60));
        assertFalse(AbsorptionRule.attunes(true, false, 80.0F, 60));
        assertFalse(AbsorptionRule.attunes(true, true, 59.0F, 60));
    }
}
