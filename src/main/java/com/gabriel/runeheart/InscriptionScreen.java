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
import java.util.Map;
import java.util.Set;

/**
 * Tela de inscrição: escolher uma magia do Block Clover equipada na combat bar (menos Anti-Magia e Tempo) e as
 * runas inscritas nela. Ao lançar a magia, as runas valem sozinhas, com a força da mana natural do lugar.
 */
public class InscriptionScreen extends Screen {
    private static final int W = 62, H = 20, GAP = 2, SPELL_W = 100, PER_ROW = 4;
    /** Runas que podem ser inscritas, na ordem dos botões: primeiro as que melhoram, depois as que somam efeito. */
    private static final Rune[] ROW_IMPROVE = { Rune.POWER, Rune.AMPLIFY, Rune.MULTIPLE, Rune.DURATION, Rune.TRIGGER };
    private static final Rune[] ROW_EFFECT = { Rune.SEAL, Rune.IMPULSE, Rune.HEAL, Rune.BARRIER };

    private int spell = 0;
    private final Set<Rune> runes = EnumSet.noneOf(Rune.class);
    private final List<Button> spellButtons = new ArrayList<>();
    private final Map<Rune, Button> runeButtons = new EnumMap<>(Rune.class);
    private int runesY;

    public InscriptionScreen() {
        super(new TranslationTextComponent("screen.runeheart.inscriptions"));
        load();
    }

    private boolean hasSpells() {
        return ClientRunes.spellKeys.length > 0;
    }

    private void load() {
        runes.clear();
        if (spell >= ClientRunes.spellKeys.length) spell = 0;
        if (!hasSpells()) return;
        Inscription i = Inscription.parse(ClientRunes.spellInscriptions[spell]);
        if (i != null) runes.addAll(i.runes);
    }

    void onSync() {
        int before = spellButtons.size();
        if (before != ClientRunes.spellKeys.length) {
            init(minecraft, width, height);                                   // a lista de magias mudou
            return;
        }
        refresh();
    }

    @Override
    protected void init() {
        buttons.clear();
        children.clear();
        spellButtons.clear();
        runeButtons.clear();
        int cx = width / 2;
        int y = 40;
        int n = ClientRunes.spellKeys.length;
        for (int i = 0; i < n; i++) {
            final int idx = i;
            int row = i / PER_ROW, col = i % PER_ROW;
            int inRow = Math.min(PER_ROW, n - row * PER_ROW);
            int left = cx - (inRow * SPELL_W + (inRow - 1) * GAP) / 2;
            spellButtons.add(addButton(new Button(left + col * (SPELL_W + GAP), y + row * (H + GAP), SPELL_W, H,
                    StringTextComponent.EMPTY, btn -> {
                        spell = idx;
                        load();
                        refresh();
                    })));
        }
        int rows = Math.max(1, (n + PER_ROW - 1) / PER_ROW);
        runesY = y + rows * (H + GAP) + 22;
        int rowW = 5 * W + 4 * GAP;
        int left = cx - rowW / 2;
        for (int i = 0; i < ROW_IMPROVE.length; i++) addRune(ROW_IMPROVE[i], left + i * (W + GAP), runesY);
        int left2 = cx - (4 * W + 3 * GAP) / 2;
        for (int i = 0; i < ROW_EFFECT.length; i++) addRune(ROW_EFFECT[i], left2 + i * (W + GAP), runesY + 34);

        int by = runesY + 34 + H + 30;
        addButton(new Button(cx - 3 * 50 / 2 - GAP, by, 50, H, new TranslationTextComponent("screen.runeheart.save"), btn -> {
            if (!hasSpells()) return;
            String text = runes.isEmpty() ? "" : new Inscription(runes).encode();
            ClientRunes.send(RuneActionPacket.INSCRIBE, 0, ClientRunes.spellKeys[spell] + "|" + text);
        }));
        addButton(new Button(cx - 50 / 2, by, 50, H, new TranslationTextComponent("screen.runeheart.clear"), btn -> {
            runes.clear();
            refresh();
        }));
        addButton(new Button(cx + 50 / 2 + GAP, by, 50, H, new TranslationTextComponent("gui.done"), btn -> onClose()));
        addButton(new Button(cx - 70, by + H + GAP, 140, H, new TranslationTextComponent("screen.runeheart.to_arrangements"),
                btn -> minecraft.setScreen(new ArrangementScreen())));
        refresh();
    }

    private void addRune(Rune r, int x, int y) {
        runeButtons.put(r, addButton(new Button(x, y, W, H, StringTextComponent.EMPTY, btn -> {
            if (!runes.remove(r)) runes.add(r);
            refresh();
        })));
    }

    private void refresh() {
        for (int i = 0; i < spellButtons.size(); i++) {
            Button b = spellButtons.get(i);
            boolean inscribed = !ClientRunes.spellInscriptions[i].isEmpty();
            IFormattableTextComponent label = new StringTextComponent(shorten(ClientRunes.spellNames[i], 15));
            if (inscribed) label = new StringTextComponent("✦ ").append(label);
            if (i == spell) label.withStyle(TextFormatting.YELLOW);
            b.setMessage(label);
        }
        for (Map.Entry<Rune, Button> e : runeButtons.entrySet()) {
            Rune r = e.getKey();
            Button b = e.getValue();
            boolean knows = (ClientRunes.known & r.bit()) != 0;
            b.active = knows && hasSpells();
            IFormattableTextComponent label = knows ? new TranslationTextComponent(r.langKey()) : new StringTextComponent("???");
            if (runes.contains(r)) label = new StringTextComponent("✔ ").append(label).withStyle(TextFormatting.GREEN);
            b.setMessage(label);
        }
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        renderBackground(ms);
        int cx = width / 2;
        drawCenteredString(ms, font, title, cx, 12, 0xFFFFFF);
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.mastery", ClientRunes.casts,
                ClientRunes.maxRunes, ClientRunes.slots), cx, 25, 0xA0A0A0);
        if (!hasSpells()) {
            drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.no_spells"), cx, 46, 0xFF8080);
        }
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.improve"), cx, runesY - 11, 0xB6F2A8);
        drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.on_hit"), cx, runesY + 23, 0xB6F2A8);
        if (hasSpells()) {
            Inscription i = new Inscription(runes);
            String problem = i.problem(ClientRunes.maxRunes, ClientRunes.known);
            drawCenteredString(ms, font, new TranslationTextComponent("screen.runeheart.insc_info", ClientRunes.spellNames[spell],
                    i.size(), ClientRunes.maxRunes), cx, runesY + 34 + H + 8, problem == null ? 0xFFFFFF : 0xFF6060);
            if (problem != null) {
                drawCenteredString(ms, font, new TranslationTextComponent("msg.runeheart." + problem, ClientRunes.maxRunes),
                        cx, runesY + 34 + H + 18, 0xFF6060);
            }
        }
        super.render(ms, mouseX, mouseY, partial);
        for (Map.Entry<Rune, Button> e : runeButtons.entrySet()) {
            if (e.getValue().isHovered() && (ClientRunes.known & e.getKey().bit()) != 0) {
                renderTooltip(ms, new TranslationTextComponent(e.getKey().langKey() + ".insc"), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
