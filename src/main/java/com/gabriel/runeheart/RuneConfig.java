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
    public static final ForgeConfigSpec.BooleanValue ABS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ABS_MANA_PERCENT;
    public static final ForgeConfigSpec.DoubleValue ABS_NATURAL_COST;
    public static final ForgeConfigSpec.IntValue ABS_OVERLOAD_BELOW;
    public static final ForgeConfigSpec.DoubleValue ABS_OVERLOAD_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ABS_MOVE_TOLERANCE;
    public static final ForgeConfigSpec.BooleanValue ABS_REQUIRE_ATTUNEMENT;
    public static final ForgeConfigSpec.IntValue ABS_ATTUNEMENT_SECONDS;
    public static final ForgeConfigSpec.IntValue ABS_ATTUNEMENT_DENSITY;
    public static final ForgeConfigSpec.BooleanValue MM_ENABLED;
    public static final ForgeConfigSpec.DoubleValue MM_IGNITION_PERCENT;
    public static final ForgeConfigSpec.DoubleValue MM_NATURAL_COST;
    public static final ForgeConfigSpec.IntValue MM_FIZZLE_BELOW;
    public static final ForgeConfigSpec.IntValue MM_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MM_MASTERY_THREE;
    public static final ForgeConfigSpec.IntValue MM_MASTERY_FIVE;
    public static final ForgeConfigSpec.DoubleValue MM_POWER_MULT;
    public static final ForgeConfigSpec.DoubleValue MM_DURATION_MULT;
    public static final ForgeConfigSpec.DoubleValue MM_AREA_RADIUS;
    public static final ForgeConfigSpec.DoubleValue MM_BOLT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue MM_BOLT_SPEED;
    public static final ForgeConfigSpec.DoubleValue MM_BARRIER_ABSORPTION;
    public static final ForgeConfigSpec.DoubleValue MM_BARRIER_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MM_SEAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MM_RANGE;
    public static final ForgeConfigSpec.DoubleValue MM_HEAL;
    public static final ForgeConfigSpec.DoubleValue MM_IMPULSE;
    public static final ForgeConfigSpec.DoubleValue MM_TRAP_SECONDS;
    public static final ForgeConfigSpec.IntValue MM_TRAPS_PER_PLAYER;
    public static final ForgeConfigSpec.DoubleValue MM_SCROLL_CHANCE;
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

        section(s, "absorption", true, "Absor\u00e7\u00e3o: segurando a tecla de concentra\u00e7\u00e3o, parado, o jogador puxa a mana natural do lugar para recarregar a pr\u00f3pria mana");
        ABS_ENABLED = bool(s, "enabled", "Liga/desliga a absor\u00e7\u00e3o", true);
        ABS_MANA_PERCENT = dbl(s, "mana_percent_per_second", "Mana recuperada por segundo com densidade 100, em porcentagem da mana m\u00e1xima. Cai junto com a densidade (densidade 50 = metade)", 3.0, 0.0, 100.0);
        ABS_NATURAL_COST = dbl(s, "natural_cost_per_second", "Quanto a densidade do chunk cai por segundo de absor\u00e7\u00e3o (enquanto a mana do jogador est\u00e1 enchendo)", 2.0, 0.0, 100.0);
        ABS_OVERLOAD_BELOW = integer(s, "overload_below", "Abaixo dessa densidade, concentrar n\u00e3o rende mana: causa n\u00e1usea e dano", 10, 0, 100);
        ABS_OVERLOAD_DAMAGE = dbl(s, "overload_damage", "Dano a cada 2 segundos de sobrecarga", 1.0, 0.0, 20.0);
        ABS_MOVE_TOLERANCE = dbl(s, "move_tolerance", "Quanto o jogador pode se mexer por tick (em blocos) sem quebrar a concentra\u00e7\u00e3o", 0.15, 0.0, 5.0);
        ABS_REQUIRE_ATTUNEMENT = bool(s, "require_attunement", "true = antes de absorver, o jogador precisa 'sentir a mana natural': ficar parado com o Mana Sense ligado num lugar denso", true);
        ABS_ATTUNEMENT_SECONDS = integer(s, "attunement_seconds", "Segundos (somados, n\u00e3o precisam ser seguidos) parado com o Mana Sense ligado num lugar denso para aprender a absor\u00e7\u00e3o", 120, 0, 36000);
        ABS_ATTUNEMENT_DENSITY = integer(s, "attunement_density", "Densidade m\u00ednima do lugar para a sintonia avan\u00e7ar", 60, 0, 100);
        s.pop();

        section(s, "mana_method", true, "Mana Method: arranjos de runas. A pr\u00f3pria mana s\u00f3 acende o arranjo; a for\u00e7a vem da mana natural do lugar (densidade 100 = for\u00e7a 1)");
        MM_ENABLED = bool(s, "enabled", "Liga/desliga o Mana Method", true);
        MM_IGNITION_PERCENT = dbl(s, "ignition_percent", "Mana do jogador gasta para acender um arranjo, em porcentagem da mana m\u00e1xima", 4.0, 0.0, 100.0);
        MM_NATURAL_COST = dbl(s, "natural_cost_per_rune", "Quanto a densidade do chunk cai por runa do arranjo conjurado", 3.0, 0.0, 100.0);
        MM_FIZZLE_BELOW = integer(s, "fizzle_below", "Abaixo dessa densidade o arranjo se desfaz (a mana de igni\u00e7\u00e3o \u00e9 perdida)", 10, 0, 100);
        MM_COOLDOWN = integer(s, "cooldown_ticks", "Espera entre duas conjura\u00e7\u00f5es (20 ticks = 1 s)", 20, 0, 6000);
        MM_MASTERY_THREE = integer(s, "mastery_three", "Conjura\u00e7\u00f5es completas para os arranjos aceitarem 3 runas", 50, 0, 100000);
        MM_MASTERY_FIVE = integer(s, "mastery_five", "Conjura\u00e7\u00f5es completas para os arranjos aceitarem 5 runas e o jogador ganhar 5 slots", 200, 0, 100000);
        MM_POWER_MULT = dbl(s, "power_multiplier", "Quanto a runa Pot\u00eancia multiplica a for\u00e7a", 1.6, 1.0, 10.0);
        MM_DURATION_MULT = dbl(s, "duration_multiplier", "Quanto a runa Dura\u00e7\u00e3o multiplica os tempos", 2.0, 1.0, 10.0);
        MM_AREA_RADIUS = dbl(s, "area_radius", "Raio, em blocos, dos efeitos com a runa Amplia\u00e7\u00e3o", 5.0, 1.0, 32.0);
        MM_BOLT_DAMAGE = dbl(s, "bolt_damage", "Dano do Disparo com for\u00e7a 1", 8.0, 0.0, 200.0);
        MM_BOLT_SPEED = dbl(s, "bolt_speed", "Velocidade do proj\u00e9til", 1.6, 0.2, 5.0);
        MM_BARRIER_ABSORPTION = dbl(s, "barrier_absorption", "Pontos de absor\u00e7\u00e3o (2 = um cora\u00e7\u00e3o) da Barreira com for\u00e7a 1", 8.0, 0.0, 200.0);
        MM_BARRIER_SECONDS = dbl(s, "barrier_seconds", "Quanto tempo a Barreira dura", 10.0, 0.5, 600.0);
        MM_SEAL_SECONDS = dbl(s, "seal_seconds", "Quanto tempo o Selo prende o alvo", 4.0, 0.5, 600.0);
        MM_RANGE = dbl(s, "range", "Alcance, em blocos, do Selo e da coloca\u00e7\u00e3o de armadilhas", 16.0, 2.0, 64.0);
        MM_HEAL = dbl(s, "heal_amount", "Vida curada (2 = um cora\u00e7\u00e3o) com for\u00e7a 1", 6.0, 0.0, 200.0);
        MM_IMPULSE = dbl(s, "impulse_strength", "For\u00e7a do empurr\u00e3o com for\u00e7a 1 (agachado puxa)", 1.5, 0.0, 10.0);
        MM_TRAP_SECONDS = dbl(s, "trap_seconds", "Quanto tempo uma armadilha do Gatilho espera algu\u00e9m pisar", 60.0, 1.0, 3600.0);
        MM_TRAPS_PER_PLAYER = integer(s, "traps_per_player", "Armadilhas ativas ao mesmo tempo por jogador (a mais antiga some)", 3, 1, 50);
        MM_SCROLL_CHANCE = dbl(s, "scroll_chest_chance", "Chance de um ba\u00fa de estrutura (masmorra, templo, mina, fortaleza...) ter um pergaminho de runa", 0.25, 0.0, 1.0);
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
