package com.gabriel.runeheart;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrangementTest {
    private static final int ALL = (1 << Rune.values().length) - 1;

    @Test
    void roundTrip() {
        Arrangement a = new Arrangement(Rune.BOLT, EnumSet.of(Rune.MULTIPLE, Rune.POWER));
        assertEquals("bolt+power+multiple", a.encode());                 // modificadoras na ordem das runas
        Arrangement b = Arrangement.parse(a.encode());
        assertEquals(Rune.BOLT, b.core);
        assertEquals(a.modifiers, b.modifiers);
        assertEquals(30, b.channelTicks());
    }

    @Test
    void badTextIsAnEmptySlot() {
        assertNull(Arrangement.parse(""));
        assertNull(Arrangement.parse(null));
        assertNull(Arrangement.parse("power+bolt"));                     // núcleo tem que vir primeiro
        assertNull(Arrangement.parse("bolt+heal"));                      // dois núcleos
        assertNull(Arrangement.parse("bolt+power+power"));               // repetida
        assertNull(Arrangement.parse("bolt+xyz"));
    }

    @Test
    void rules() {
        Arrangement big = Arrangement.parse("seal+amplify+duration");
        assertEquals("arr_too_many", big.problem(2, ALL));
        assertNull(big.problem(3, ALL));
        assertEquals("arr_unknown_rune", big.problem(5, Rune.SEAL.bit() | Rune.AMPLIFY.bit()));
        assertEquals("arr_multiple_needs_bolt", Arrangement.parse("heal+multiple").problem(5, ALL));
    }

    @Test
    void withoutTrigger() {
        Arrangement trap = Arrangement.parse("seal+trigger+duration");
        Arrangement inside = trap.without(Rune.TRIGGER);
        assertFalse(inside.has(Rune.TRIGGER));
        assertTrue(inside.has(Rune.DURATION));
        assertTrue(trap.has(Rune.TRIGGER));                              // o original não muda
    }

    @Test
    void mastery() {
        assertEquals(2, Mastery.maxRunes(0, 50, 200));
        assertEquals(3, Mastery.maxRunes(50, 50, 200));
        assertEquals(5, Mastery.maxRunes(200, 50, 200));
        assertEquals(3, Mastery.slots(199, 200));
        assertEquals(5, Mastery.slots(200, 200));
        assertTrue(Mastery.levelsUp(49, 50, 200));
        assertFalse(Mastery.levelsUp(50, 50, 200));
    }
}
