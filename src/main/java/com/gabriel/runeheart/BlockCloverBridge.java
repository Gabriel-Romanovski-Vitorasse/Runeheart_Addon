package com.gabriel.runeheart;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Acessa o Block Clover por reflexão (sem depender do .jar dele para compilar). Se algo mudar nas classes do mod,
 * as chamadas falham em silêncio e o recurso correspondente não faz nada.
 * <p>
 * Regra do addon: não mexer na lista de habilidades nem na combat bar do Block Clover (isso bagunça o mod base).
 * Aqui só se lê o estado dele e se gasta ou devolve mana.
 */
public final class BlockCloverBridge {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "block_clover";

    // ---- Nomes do Block Clover usados pelo addon (todos aqui, para achar fácil quando o mod mudar) ----
    private static final String PKG = "com.yuanno.block_clover.";
    public static final String CLS_MANA_SENSE = PKG + "spells.misc.ManaSenseAbility";
    public static final String CLS_CONTINUOUS = PKG + "api.ability.sorts.ContinuousAbility";
    /** Toda Mana Zone do Block Clover (uma por atributo) tem este pedaço no nome da classe. */
    public static final String MANA_ZONE_NAME = "ManaZone";

    private static boolean tried, statsOk, abilitiesOk, syncOk;
    private static Method statsGet, getMana, setMana, getMaxMana;
    private static Method abilityGet, getUnlockedAll, getUnlockedByCore, isContinuous;
    private static Object coreManaSense;
    private static Method sendTo;
    private static Constructor<?> statsPacket;

    private BlockCloverBridge() {
    }

    private static void init() {
        if (tried) return;
        tried = true;
        try {
            Class<?> capStats = Class.forName(PKG + "data.entity.EntityStatsCapability");
            Class<?> stats = Class.forName(PKG + "data.entity.IEntityStats");
            statsGet = capStats.getMethod("get", LivingEntity.class);
            getMana = stats.getMethod("getMana");
            setMana = stats.getMethod("setMana", float.class);
            getMaxMana = stats.getMethod("getMaxMana");
            statsOk = true;

            try {
                Class<?> capAbility = Class.forName(PKG + "data.ability.AbilityDataCapability");
                Class<?> data = Class.forName(PKG + "data.ability.IAbilityData");
                Class<?> core = Class.forName(PKG + "api.ability.AbilityCore");
                abilityGet = capAbility.getMethod("get", LivingEntity.class);
                getUnlockedAll = data.getMethod("getUnlockedAbilities");
                getUnlockedByCore = data.getMethod("getUnlockedAbility", core);
                isContinuous = Class.forName(CLS_CONTINUOUS).getMethod("isContinuous");
                coreManaSense = Class.forName(CLS_MANA_SENSE).getField("INSTANCE").get(null);
                abilitiesOk = true;
            } catch (Throwable ignored) {
                // sem habilidades: Mana Sense e Mana Zone contam como ausentes
            }

            try {
                Class<?> ph = Class.forName(PKG + "networking.PacketHandler");
                sendTo = ph.getMethod("sendTo", Object.class, PlayerEntity.class);
                statsPacket = Class.forName(PKG + "networking.server.SSyncEntityStatsPacket").getConstructor(int.class, stats);
                syncOk = true;
            } catch (Throwable ignored) {
                // sem sincronização: a barra de mana do cliente só atualiza quando o Block Clover sincronizar
            }
        } catch (Throwable t) {
            statsOk = false;
        }
    }

    /** As estatísticas do Block Clover (mana) foram encontradas. */
    public static boolean isAvailable() {
        init();
        return statsOk;
    }

    private static Object stats(LivingEntity e) throws Exception {
        return statsGet.invoke(null, e);
    }

    /** Mana atual, ou -1 se não deu para ler. */
    public static float getMana(LivingEntity entity) {
        init();
        if (!statsOk) return -1.0F;
        try {
            Object st = stats(entity);
            return st == null ? -1.0F : (Float) getMana.invoke(st);
        } catch (Throwable t) {
            return -1.0F;
        }
    }

    /** Mana máxima, ou -1 se não deu para ler. */
    public static float getMaxMana(LivingEntity entity) {
        init();
        if (!statsOk) return -1.0F;
        try {
            Object st = stats(entity);
            return st == null ? -1.0F : (Float) getMaxMana.invoke(st);
        } catch (Throwable t) {
            return -1.0F;
        }
    }

    /**
     * Soma {@code amount} à mana (negativo gasta), sem passar do máximo nem ficar abaixo de zero.
     *
     * @return quanto realmente mudou (0 se não deu para mexer)
     */
    public static float addMana(LivingEntity entity, float amount) {
        init();
        if (!statsOk) return 0.0F;
        try {
            Object st = stats(entity);
            if (st == null) return 0.0F;
            float now = (Float) getMana.invoke(st);
            float next = ManaMath.clamp(now + amount, 0.0F, (Float) getMaxMana.invoke(st));
            setMana.invoke(st, next);
            return next - now;
        } catch (Throwable t) {
            return 0.0F;
        }
    }

    /** O jogador tem o Mana Sense do Block Clover e ele está ligado agora. */
    public static boolean isManaSenseActive(PlayerEntity player) {
        init();
        if (!abilitiesOk) return false;
        try {
            Object data = abilityGet.invoke(null, player);
            Object sense = data == null ? null : getUnlockedByCore.invoke(data, coreManaSense);
            return sense != null && (Boolean) isContinuous.invoke(sense);
        } catch (Throwable t) {
            return false;
        }
    }

    /** O jogador já aprendeu alguma Mana Zone do Block Clover (de qualquer atributo). */
    public static boolean knowsManaZone(PlayerEntity player) {
        init();
        if (!abilitiesOk) return false;
        try {
            Object data = abilityGet.invoke(null, player);
            if (data == null) return false;
            for (Object ability : (List<?>) getUnlockedAll.invoke(data)) {
                if (ability != null && ability.getClass().getSimpleName().contains(MANA_ZONE_NAME)) return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Reenvia a mana ao cliente (a barra de mana do Block Clover). */
    public static void syncToClient(PlayerEntity player) {
        init();
        if (!syncOk) return;
        try {
            Object st = stats(player);
            if (st != null) sendTo.invoke(null, statsPacket.newInstance(player.getId(), st), player);
        } catch (Throwable ignored) {
        }
    }

    /** Escreve no log a versão do addon e o que foi encontrado no Block Clover. */
    public static void logStatus() {
        String version = ModList.get().getModContainerById(RuneheartMod.MODID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("?");
        LOGGER.info("[Runeheart] versão {}", version);
        if (!ModList.get().isLoaded(MOD_ID)) {
            LOGGER.warn("[Runeheart] Block Clover nao esta instalado: as tecnicas de mana ficam desligadas.");
            return;
        }
        init();
        LOGGER.info("[Runeheart] Block Clover: mana {}, habilidades {}, sincronizacao {}", ok(statsOk), ok(abilitiesOk), ok(syncOk));
        if (!statsOk || !abilitiesOk || !syncOk) {
            LOGGER.warn("[Runeheart] Parte do Block Clover nao foi encontrada (versao diferente?). "
                    + "Os recursos que dependem dela ficam desligados.");
        }
    }

    private static String ok(boolean b) {
        return b ? "ok" : "AUSENTE";
    }
}
