package com.gabriel.runeheart;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tela de arranjos (tecla J): escolher o slot, a runa núcleo e as modificadoras, e salvar. O slot editado vira o
 * escolhido para conjurar. Runas que o jogador ainda não aprendeu aparecem desligadas.
 */
public class ArrangementScreen extends Screen {
    private static final int W = 62, H = 20, GAP = 2;

    private int slot;
    private Rune core;
    private final Set<Rune> mods = EnumSet.noneOf(Rune.class);
    private final List<Button> slotButtons = new ArrayList<>();
    private final Map<Rune, Button> runeButtons = new EnumMap<>(Rune.class);

    public ArrangementScreen() {
        super(new TranslationTextComponent("screen.runeheart.arrangements"));
        slot = ClientRunes.selected;
        load();
    }

    private void load() {
        mods.clear();
        Arrangement a = Arrangement.parse(ClientRunes.texts[slot]);
        core = a == null ? null : a.core;
        if (a != null) mods.addAll(a.modifiers);
    }

    /** O servidor mandou o estado novo (ex.: depois de salvar). */
    void onSync() {
        if (slot >= ClientRunes.slots) slot = 0;
        refresh();
    }

    private Arrangement current() {
        return core == null ? null : new Arrangement(core, mods);
    }

    @Override
    protected void init() {
        slotButtons.clear();
        runeButtons.clear();
        int cx = width / 2, rowW = 5 * W + 4 * GAP, left = cx - rowW / 2;
        int y = height / 2 - 78;

        for (int i = 0; i < RuneData.MAX_SLOTS; i++) {
            final int s = i;
            Button b = addButton(new Button(left + i * (W + GAP), y, W, H, StringTextComponent.EMPTY, btn -> {
                slot = s;
                load();
                ClientRunes.send(RuneActionPacket.SELECT, s, "");
                refresh();
            }));
            slotButtons.add(b);
        }
        y += 34;
        int col = 0;
        for (Rune r : Rune.values()) {
            if (!r.core) continue;
            runeButtons.put(r, addButton(new Button(left + col++ * (W + GAP), y, W, H, StringTextComponent.EMPTY, btn -> {
                core = core == r ? null : r;
                if (core != Rune.BOLT) mods.remove(Rune.MULTIPLE);
                refresh();
            })));
        }
        y += 34;
        col = 0;
        for (Rune r : Rune.values()) {
            if (r.core) continue;
            runeButtons.put(r, addButton(new Button(left + col++ * (W + GAP), y, W, H, StringTextComponent.EMPTY, btn -> {
                if (!mods.remove(r)) mods.add(r);
                refresh();
            })));
        }
        y += 58;
        addButton(new Button(cx - 3 * 50 / 2 - GAP, y, 50, H, new TranslationTextComponent("screen.runeheart.save"), btn -> {
            Arrangement a = current();
            ClientRunes.send(RuneActionPacket.SAVE, slot, a == null ? "" : a.encode());
        }));
        addButton(new Button(cx - 50 / 2, y, 50, H, new TranslationTextComponent("screen.runeheart.clear"), btn -> {
            core = null;
            mods.clear();
            refresh();
        }));
        addButton(new Button(cx + 50 / 2 + GAP, y, 50, H, new TranslationTextComponent("gui.done"), btn -> onClose()));
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < slotButtons.size(); i++) {
            Button b = slotButtons.get(i);
            b.visible = i < ClientRunes.slots;
            boolean filled = !ClientRunes.texts[i].isEmpty();
            IFormattableTextComponent label = new TranslationTextComponent("screen.runeheart.slot", i + 1);
            if (i == slot) label = new StringTextComponent("▶ ").append(label).withStyle(TextFormatting.YELLOW);
            else if (!filled) label.withStyle(TextFormatting.GRAY);
            b.setMessage(label);
        }
        for (Map.Entry<Rune, Button> e : runeButtons.entrySet()) {
            Rune r = e.getKey();
            Button b = e.getValue();
            boolean knows = (ClientRunes.known & r.bit()) != 0;
            boolean on = r.core ? core == r : mods.contains(r);
            b.active = knows && (r != Rune.MULTIPLE || core == Rune.BOLT);
            IFormattableTextComponent label = knows ? new TranslationTextComponent(r.langKey()) : new StringTextComponent("???");
            if (on) label = new StringTextComponent("✔ ").append(label).withStyle(TextFormatting.GREEN);
            b.setMessage(label);
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        renderBackground(ms);
        int cx = width / 2, y = height / 2 - 78;
        drawCenteredString(ms, font, title, cx, y - 26, 0xFFFFFF);
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.mastery", ClientRunes.casts,
                ClientRunes.maxRunes, ClientRunes.slots), cx, y - 13, 0xA0A0A0);
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.cores"), cx, y + 24, 0xB6F2A8);
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.modifiers"), cx, y + 58, 0xB6F2A8);

        Arrangement a = current();
        int infoY = y + 96;
        if (a == null) {
            drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.pick_core"), cx, infoY, 0xA0A0A0);
        } else {
            String problem = a.problem(ClientRunes.maxRunes, ClientRunes.known);
            String seconds = String.format(Locale.ROOT, "%.1f", a.channelTicks() / 20.0D);
            drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.info", a.size(), ClientRunes.maxRunes, seconds),
                    cx, infoY, problem == null ? 0xFFFFFF : 0xFF6060);
            if (problem != null) {
                drawCenteredString(ms, font, new TranslationTextComponent("msg.runeheart." + problem, ClientRunes.maxRunes),
                        cx, infoY + 12, 0xFF6060);
            }
        }
        super.render(ms, mouseX, mouseY, partial);
        // descrição da runa sob o mouse
        for (Map.Entry<Rune, Button> e : runeButtons.entrySet()) {
            if (e.getValue().isHovered() && (ClientRunes.known & e.getKey().bit()) != 0) {
                renderTooltip(ms, new TranslationTextComponent(e.getKey().langKey() + ".desc"), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
