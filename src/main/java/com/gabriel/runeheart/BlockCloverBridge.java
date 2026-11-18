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
    public static final String CLS_ABILITY_DAMAGE = PKG + "damagesource.AbilityDamageSource";
    public static final String CLS_PROJECTILE = PKG + "api.ability.AbilityProjectileEntity";
    /** Toda Mana Zone do Block Clover (uma por atributo) tem este pedaço no nome da classe. */
    public static final String MANA_ZONE_NAME = "ManaZone";

    private static boolean tried, statsOk, abilitiesOk, syncOk;
    private static Method statsGet, getMana, setMana, getMaxMana;
    private static Method abilityGet, getUnlockedAll, getUnlockedByCore, getEquippedAll, isContinuous;
    private static Class<?> manaSenseClass;
    private static Object coreManaSense;
    private static Method sendTo;
    // inscri\u00e7\u00e3o: de qual magia veio um dano ou um proj\u00e9til
    private static boolean spellsOk;
    private static Class<?> damageClass, projectileClass;
    private static Method damageAbility, coreAbilityClass, coreName, abilityGetCore;
    private static Method projDamage, projMaxLife, projSetMaxLife, projSetThrower;
    private static java.lang.reflect.Field projSource;
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
                getEquippedAll = data.getMethod("getEquippedAbilities");
                isContinuous = Class.forName(CLS_CONTINUOUS).getMethod("isContinuous");
                manaSenseClass = Class.forName(CLS_MANA_SENSE);
                coreManaSense = manaSenseClass.getField("INSTANCE").get(null);
                abilitiesOk = true;
            } catch (Throwable ignored) {
                // sem habilidades: Mana Sense e Mana Zone contam como ausentes
            }

            try {
                Class<?> core = Class.forName(PKG + "api.ability.AbilityCore");
                damageClass = Class.forName(CLS_ABILITY_DAMAGE);
                damageAbility = damageClass.getMethod("getAbilitySource");
                coreAbilityClass = core.getMethod("getAbilityClass");
                coreName = core.getMethod("getName");
                abilityGetCore = Class.forName(PKG + "api.ability.Ability").getMethod("getCore");
                projectileClass = Class.forName(CLS_PROJECTILE);
                projSource = projectileClass.getField("source");
                projDamage = projectileClass.getMethod("getDamage");
                projMaxLife = projectileClass.getMethod("getMaxLife");
                projSetMaxLife = projectileClass.getMethod("setMaxLife", int.class);
                projSetThrower = projectileClass.getMethod("setThrower", LivingEntity.class);
                spellsOk = true;
            } catch (Throwable ignored) {
                // sem isso a inscri\u00e7\u00e3o n\u00e3o tem como saber de qual magia veio um golpe: fica desligada
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

    /**
     * O jogador tem o Mana Sense do Block Clover ligado agora.
     * <p>
     * O estado "ligado" fica na c\u00f3pia da habilidade que est\u00e1 na combat bar (a equipada), que nem sempre \u00e9 o mesmo
     * objeto da lista de habilidades aprendidas: por isso olha primeiro as equipadas.
     */
    public static boolean isManaSenseActive(PlayerEntity player) {
        init();
        if (!abilitiesOk) return false;
        try {
            Object data = abilityGet.invoke(null, player);
            if (data == null) return false;
            for (Object ability : (List<?>) getEquippedAll.invoke(data)) {
                if (manaSenseClass.isInstance(ability) && (Boolean) isContinuous.invoke(ability)) return true;
            }
            Object sense = getUnlockedByCore.invoke(data, coreManaSense);
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

    // ---------- Magias (inscri\u00e7\u00e3o) ----------

    /** Uma magia do Block Clover: chave salva no jogador (nome da classe) e nome para mostrar. */
    public static final class Spell {
        public final String key, name;

        Spell(String key, String name) {
            this.key = key;
            this.name = name;
        }
    }

    /** Magia de um "AbilityCore" do Block Clover, se ela aceita inscri\u00e7\u00e3o (sen\u00e3o null). */
    private static Spell spellOfCore(Object core) throws Exception {
        if (core == null) return null;
        Class<?> cls = (Class<?>) coreAbilityClass.invoke(core);
        if (cls == null || !Inscription.eligible(cls.getName())) return null;
        return new Spell(cls.getSimpleName(), String.valueOf(coreName.invoke(core)));
    }

    /** De qual magia (que aceita inscri\u00e7\u00e3o) veio este dano, ou null. */
    public static Spell spellOfDamage(net.minecraft.util.DamageSource source) {
        init();
        if (!spellsOk || source == null || !damageClass.isInstance(source)) return null;
        try {
            return spellOfCore(damageAbility.invoke(source));
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isSpellProjectile(net.minecraft.entity.Entity e) {
        init();
        return spellsOk && e != null && projectileClass.isInstance(e);
    }

    /** De qual magia (que aceita inscri\u00e7\u00e3o) veio este proj\u00e9til, ou null. */
    public static Spell spellOfProjectile(net.minecraft.entity.Entity e) {
        if (!isSpellProjectile(e)) return null;
        try {
            return spellOfDamage((net.minecraft.util.DamageSource) projSource.get(e));
        } catch (Throwable t) {
            return null;
        }
    }

    public static float projectileDamage(net.minecraft.entity.Entity e) {
        if (!isSpellProjectile(e)) return 0.0F;
        try {
            return (Float) projDamage.invoke(e);
        } catch (Throwable t) {
            return 0.0F;
        }
    }

    /** Multiplica o tempo de vida (alcance) do proj\u00e9til. */
    public static void scaleProjectileLife(net.minecraft.entity.Entity e, double factor) {
        if (!isSpellProjectile(e)) return;
        try {
            int life = (Integer) projMaxLife.invoke(e);
            if (life > 0) projSetMaxLife.invoke(e, (int) Math.round(life * factor));
        } catch (Throwable ignored) {
        }
    }

    /** Copia para {@code to} o dono e a fonte de dano do proj\u00e9til original (o que o NBT n\u00e3o carrega). */
    public static void copyProjectileLinks(net.minecraft.entity.Entity from, net.minecraft.entity.Entity to, LivingEntity owner) {
        if (!isSpellProjectile(from) || !isSpellProjectile(to)) return;
        try {
            projSource.set(to, projSource.get(from));
            java.lang.reflect.Field bypass = projectileClass.getField("bypassingSource");
            bypass.set(to, bypass.get(from));
            if (owner != null) projSetThrower.invoke(to, owner);
        } catch (Throwable ignored) {
        }
    }

    /** Magias equipadas na combat bar que aceitam inscri\u00e7\u00e3o (sem repetir). */
    public static List<Spell> equippedSpells(PlayerEntity player) {
        init();
        List<Spell> list = new java.util.ArrayList<>();
        if (!abilitiesOk || !spellsOk) return list;
        try {
            Object data = abilityGet.invoke(null, player);
            if (data == null) return list;
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Object ability : (List<?>) getEquippedAll.invoke(data)) {
                if (ability == null) continue;
                Spell s = spellOfCore(abilityGetCore.invoke(ability));
                if (s != null && seen.add(s.key)) list.add(s);
            }
        } catch (Throwable ignored) {
        }
        return list;
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
        LOGGER.info("[Runeheart] Block Clover: mana {}, habilidades {}, sincronizacao {}, magias (inscricao) {}",
                ok(statsOk), ok(abilitiesOk), ok(syncOk), ok(spellsOk));
        if (!statsOk || !abilitiesOk || !syncOk || !spellsOk) {
            LOGGER.warn("[Runeheart] Parte do Block Clover nao foi encontrada (versao diferente?). "
                    + "Os recursos que dependem dela ficam desligados.");
        }
    }

    private static String ok(boolean b) {
        return b ? "ok" : "AUSENTE";
    }
}
