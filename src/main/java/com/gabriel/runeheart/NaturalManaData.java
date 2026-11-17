package com.gabriel.runeheart;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Quanto já foi puxado de cada chunk de uma dimensão (o "déficit"), salvo com o mundo. Chunk fora daqui está cheio.
 * Cada entrada guarda o déficit e o tick em que ele foi anotado; a recarga é calculada na hora de ler.
 */
public class NaturalManaData extends WorldSavedData {
    public static final String NAME = RuneheartMod.MODID + "_natural_mana";

    static final class Entry {
        float deficit;
        long tick;

        Entry(float deficit, long tick) {
            this.deficit = deficit;
            this.tick = tick;
        }
    }

    private final Map<Long, Entry> entries = new HashMap<>();

    public NaturalManaData() {
        super(NAME);
    }

    /** Déficit do chunk agora (já descontada a recarga desde a última anotação). */
    public float deficit(long chunk, long now, int refillTicks) {
        Entry e = entries.get(chunk);
        if (e == null) return 0.0F;
        float d = NaturalManaRule.deficitAfter(e.deficit, Math.max(0, now - e.tick), refillTicks);
        if (d <= 0.0F) {
            entries.remove(chunk);
            setDirty();
        }
        return d;
    }

    /** Anota o novo déficit do chunk (0 = cheio). */
    public void setDeficit(long chunk, float deficit, long now) {
        if (deficit <= 0.0F) {
            if (entries.remove(chunk) != null) setDirty();
            return;
        }
        entries.put(chunk, new Entry(deficit, now));
        setDirty();
    }

    /** Esquece os chunks que já encheram (para o arquivo não crescer à toa). */
    public void prune(long now, int refillTicks) {
        for (Iterator<Map.Entry<Long, Entry>> it = entries.entrySet().iterator(); it.hasNext(); ) {
            Entry e = it.next().getValue();
            if (NaturalManaRule.deficitAfter(e.deficit, Math.max(0, now - e.tick), refillTicks) <= 0.0F) {
                it.remove();
                setDirty();
            }
        }
    }

    @Override
    public void load(CompoundNBT nbt) {
        entries.clear();
        ListNBT list = nbt.getList("chunks", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT c = list.getCompound(i);
            entries.put(c.getLong("pos"), new Entry(c.getFloat("deficit"), c.getLong("tick")));
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (Map.Entry<Long, Entry> e : entries.entrySet()) {
            CompoundNBT c = new CompoundNBT();
            c.putLong("pos", e.getKey());
            c.putFloat("deficit", e.getValue().deficit);
            c.putLong("tick", e.getValue().tick);
            list.add(c);
        }
        nbt.put("chunks", list);
        return nbt;
    }
}
