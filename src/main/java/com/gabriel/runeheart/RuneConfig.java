package com.gabriel.runeheart;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuração, em dois arquivos:
 * <ul>
 *   <li><b>servidor</b> ({@code <mundo>/serverconfig/runeheart-server.toml}): regras de jogo. O Forge manda uma
 *   cópia para cada cliente ao entrar, então o cliente desenha com os mesmos valores do servidor (ex.: raio da zona).
 *   Para valer em mundos novos, copie o arquivo para {@code defaultconfigs/}.</li>
 *   <li><b>cliente</b> ({@code config/runeheart-client.toml}): só visual e HUD deste computador.</li>
 * </ul>
 * O código lê os valores direto daqui ({@code RuneConfig.X.get()}); não há cópia em outras classes.
 * Antes de o arquivo carregar (ex.: no menu principal), {@code get()} devolve o valor padrão.
 * Os nomes e dicas da tela de configuração ficam nos arquivos de idioma ({@code config.runeheart.*}).
 * <p>
 * ARQUIVO GERADO por {@code tools/config/genconfig.py} a partir de {@code tools/config/opts.py}: para mudar uma
 * opção, edite o opts.py e rode o gerador (não edite este arquivo à mão).
 */
public final class RuneConfig {
    private RuneConfig() {
    }

    /** Descrição de uma opção, usada pela tela de configuração. */
    public static final class Opt {
        public static final int BOOL = 0, INT = 1, DOUBLE = 2;
        public final String section, key;
        /** true = regra do servidor; false = opção visual deste computador. */
        public final boolean server;
        public final int kind;
        public final double min, max;
        public final Object def;
        public final ForgeConfigSpec.ConfigValue<?> value;

        Opt(String section, String key, boolean server, int kind, double min, double max, Object def,
            ForgeConfigSpec.ConfigValue<?> value) {
            this.section = section; this.key = key; this.server = server;
            this.kind = kind; this.min = min; this.max = max; this.def = def; this.value = value;
        }

        public String sectionKey() { return "config." + RuneheartMod.MODID + "." + section; }
        public String labelKey() { return sectionKey() + "." + key; }
        public String tooltipKey() { return labelKey() + ".tooltip"; }
        public ForgeConfigSpec spec() { return server ? SERVER_SPEC : CLIENT_SPEC; }
    }

    private static final List<Opt> ALL = new ArrayList<>();
    /** Todas as opções, na ordem em que aparecem na tela. */
    public static final List<Opt> OPTS = Collections.unmodifiableList(ALL);
    private static String curSection = "";
    private static boolean curServer;

    private static void section(ForgeConfigSpec.Builder b, String key, boolean server, String comment) {
        b.comment(comment).push(key);
        curSection = key;
        curServer = server;
    }

    private static ForgeConfigSpec.BooleanValue bool(ForgeConfigSpec.Builder b, String key, String comment, boolean def) {
        ForgeConfigSpec.BooleanValue v = b.comment(comment).define(key, def);
        ALL.add(new Opt(curSection, key, curServer, Opt.BOOL, 0, 1, def, v));
        return v;
    }

    private static ForgeConfigSpec.IntValue integer(ForgeConfigSpec.Builder b, String key, String comment, int def, int min, int max) {
        ForgeConfigSpec.IntValue v = b.comment(comment).defineInRange(key, def, min, max);
        ALL.add(new Opt(curSection, key, curServer, Opt.INT, min, max, def, v));
        return v;
    }

    private static ForgeConfigSpec.DoubleValue dbl(ForgeConfigSpec.Builder b, String key, String comment, double def, double min, double max) {
        ForgeConfigSpec.DoubleValue v = b.comment(comment).defineInRange(key, def, min, max);
        ALL.add(new Opt(curSection, key, curServer, Opt.DOUBLE, min, max, def, v));
        return v;
    }

    /** Valor decimal como float (a maior parte do jogo usa float). */
    public static float f(ForgeConfigSpec.DoubleValue v) {
        return v.get().floatValue();
    }

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;
    public static final ForgeConfigSpec.BooleanValue NM_ENABLED;
    public static final ForgeConfigSpec.IntValue NM_REGEN_SECONDS;
    public static final ForgeConfigSpec.IntValue NM_RESCAN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue NM_VEGETATION_BONUS;
    public static final ForgeConfigSpec.DoubleValue NM_WATER_BONUS;
    public static final ForgeConfigSpec.DoubleValue NM_STORM_BONUS;
    public static final ForgeConfigSpec.DoubleValue NM_BUILDING_PENALTY;
    public static final ForgeConfigSpec.IntValue HUD_X;
    public static final ForgeConfigSpec.IntValue HUD_Y;
    public static final ForgeConfigSpec.BooleanValue PARTICLES;
    public static final ForgeConfigSpec.IntValue PARTICLE_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue SHOW_HUD;

    static {
        ForgeConfigSpec.Builder s = new ForgeConfigSpec.Builder();

        section(s, "general", true, "Regras gerais do Runeheart");
        DEBUG_LOG = bool(s, "debug_log", "true = escreve no latest.log o que o Runeheart est\u00e1 fazendo (marcador [Runeheart]), para investigar problemas", false);
        s.pop();

        section(s, "natural_mana", true, "Mana natural do ambiente: cada chunk tem uma densidade de 0 a 100 conforme o bioma, a vegeta\u00e7\u00e3o, a \u00e1gua, o clima e as constru\u00e7\u00f5es");
        NM_ENABLED = bool(s, "enabled", "Liga/desliga a mana natural", true);
        NM_REGEN_SECONDS = integer(s, "regen_seconds", "Segundos para um chunk esgotado voltar \u00e0 densidade cheia", 300, 10, 36000);
        NM_RESCAN_SECONDS = integer(s, "rescan_seconds", "De quanto em quanto tempo a vegeta\u00e7\u00e3o e as constru\u00e7\u00f5es de um chunk s\u00e3o contadas de novo", 300, 10, 36000);
        NM_VEGETATION_BONUS = dbl(s, "vegetation_bonus", "Densidade somada por um chunk coberto de vegeta\u00e7\u00e3o (folhas, troncos, flores, plantas)", 20.0, 0.0, 100.0);
        NM_WATER_BONUS = dbl(s, "water_bonus", "Densidade somada quando o chunk tem \u00e1gua corrente ou cachoeira", 10.0, 0.0, 100.0);
        NM_STORM_BONUS = dbl(s, "storm_bonus", "Densidade somada durante tempestades com raios", 15.0, 0.0, 100.0);
        NM_BUILDING_PENALTY = dbl(s, "building_penalty", "Densidade tirada de um chunk tomado por constru\u00e7\u00f5es (tijolos, t\u00e1buas, vidro, ferro, redstone)", 30.0, 0.0, 100.0);
        s.pop();
        SERVER_SPEC = s.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();

        section(c, "visual", false, "Visual e HUD (s\u00f3 neste computador)");
        HUD_X = integer(c, "hud_x", "Dist\u00e2ncia da borda esquerda da tela, em pixels da interface", 6, 0, 4000);
        HUD_Y = integer(c, "hud_y", "Dist\u00e2ncia do topo da tela, em pixels da interface", 6, 0, 4000);
        PARTICLES = bool(c, "particles", "Mostrar um p\u00f3len brilhante no ar onde a mana natural \u00e9 alta", true);
        PARTICLE_THRESHOLD = integer(c, "particle_threshold", "Densidade a partir da qual as part\u00edculas aparecem", 70, 0, 100);
        SHOW_HUD = bool(c, "show_hud", "Mostrar o medidor de mana natural quando o Mana Sense estiver ativo", true);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    /** Registra os dois arquivos (chamado no construtor do mod). */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, RuneheartMod.MODID + "-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, RuneheartMod.MODID + "-client.toml");
    }
}
