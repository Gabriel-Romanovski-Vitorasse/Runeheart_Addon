package com.gabriel.runeheart;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InscriptionTest {
    private static final int ALL = (1 << Rune.values().length) - 1;
    private static final String PKG = "com.yuanno.block_clover.spells.";

    @Test
    void roundTrip() {
        Inscription i = new Inscription(EnumSet.of(Rune.SEAL, Rune.POWER));
        assertEquals("seal+power", i.encode());                          // na ordem das runas
        assertEquals(i.runes, Inscription.parse("power+seal").runes);
        assertNull(Inscription.parse(""));
        assertNull(Inscription.parse("power+power"));
        assertNull(Inscription.parse("power+xyz"));
    }

    @Test
    void rules() {
        assertEquals("insc_no_bolt", Inscription.parse("bolt+power").problem(5, ALL));
        assertEquals("arr_too_many", Inscription.parse("power+amplify+seal").problem(2, ALL));
        assertEquals("arr_unknown_rune", Inscription.parse("power").problem(5, Rune.SEAL.bit()));
        assertNull(Inscription.parse("power+multiple").problem(2, ALL));     // Múltiplo vale sem Disparo aqui
    }

    @Test
    void damage() {
        assertEquals(1.0F, Inscription.damageMultiplier(false, 1.0F, 1.6F), 0.001F);
        assertEquals(1.6F, Inscription.damageMultiplier(true, 1.0F, 1.6F), 0.001F);
        assertEquals(1.3F, Inscription.damageMultiplier(true, 0.5F, 1.6F), 0.001F);
        assertEquals(6.0F, Inscription.parse("power+seal").naturalCost(3.0F), 0.001F);
    }

    @Test
    void whichSpells() {
        assertEquals("lightning", Inscription.attributeOf(PKG + "lightning.ThunderFiendAbility"));
        assertTrue(Inscription.eligible(PKG + "lightning.ThunderFiendAbility"));
        assertTrue(Inscription.eligible(PKG + "fire.FireBallAbility"));
        assertFalse(Inscription.eligible(PKG + "time.ChronoStasisAbility"));
        assertFalse(Inscription.eligible(PKG + "antimagic.DemonSlayerAbility"));
        assertFalse(Inscription.eligible(PKG + "misc.ManaSkinAbility"));
        assertFalse(Inscription.eligible("com.other.Thing"));
        assertNull(Inscription.attributeOf(null));
    }
}
