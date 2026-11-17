package com.gabriel.runeheart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MsgTest {
    @Test
    void oneDecimalAlwaysUsesADot() {
        assertEquals("1.5", Msg.f1(1.5));
        assertEquals("2.0", Msg.f1(2.0));
    }

    @Test
    void rateDropsUselessDecimals() {
        assertEquals("3", Msg.rate(3.0));
        assertEquals("1.5", Msg.rate(1.5));
    }
}
