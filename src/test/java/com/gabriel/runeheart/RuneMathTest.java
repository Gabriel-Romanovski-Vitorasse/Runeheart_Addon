package com.gabriel.runeheart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RuneMathTest {
    @Test
    void strengthFollowsDensityAndPower() {
        assertEquals(1.0F, RuneMath.strength(100.0F, false, 1.6F), 0.001F);
        assertEquals(0.5F, RuneMath.strength(50.0F, false, 1.6F), 0.001F);
        assertEquals(0.8F, RuneMath.strength(50.0F, true, 1.6F), 0.001F);
        assertEquals(1.0F, RuneMath.strength(140.0F, false, 1.6F), 0.001F);
    }

    @Test
    void durationMultiplies() {
        assertEquals(80, RuneMath.ticks(4.0D, false, 2.0D));
        assertEquals(160, RuneMath.ticks(4.0D, true, 2.0D));
    }

    @Test
    void multipleFansOut() {
        assertArrayEquals(new float[] { 0.0F }, RuneMath.spread(false), 0.001F);
        assertEquals(3, RuneMath.spread(true).length);
    }
}
