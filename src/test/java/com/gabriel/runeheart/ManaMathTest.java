package com.gabriel.runeheart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaMathTest {
    @Test
    void clampKeepsInside() {
        assertEquals(5.0F, ManaMath.clamp(5.0F, 0.0F, 10.0F));
        assertEquals(0.0F, ManaMath.clamp(-3.0F, 0.0F, 10.0F));
        assertEquals(10.0F, ManaMath.clamp(12.0F, 0.0F, 10.0F));
    }
}
