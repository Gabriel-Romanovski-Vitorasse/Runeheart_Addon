package com.gabriel.runeheart;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** Runeheart: mana natural do ambiente e Mana Method (runas e arranjos) para o Block Clover. */
@Mod(RuneheartMod.MODID)
public class RuneheartMod {
    public static final String MODID = "runeheart";
    /**
     * Versão do protocolo de rede. Suba o número sempre que um pacote mudar de formato: cliente e servidor
     * precisam ter o addon na MESMA versão, e quem tiver outra é recusado ao entrar, com uma mensagem clara.
     */
    private static final String PROTOCOL = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> RUNE_SCROLL = ITEMS.register("rune_scroll", RuneScrollItem::new);

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MODID);
    public static final RegistryObject<EntityType<RuneBoltEntity>> RUNE_BOLT = ENTITIES.register("rune_bolt",
            () -> EntityType.Builder.<RuneBoltEntity>of(RuneBoltEntity::new, EntityClassification.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(10).build(MODID + ":rune_bolt"));

    public RuneheartMod() {
        RuneConfig.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent e) -> {
            BlockCloverBridge.logStatus();
            e.enqueueWork(RuneLoot::registerCondition);
        });
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> RuneCommand.register(e.getDispatcher()));
        CHANNEL.registerMessage(0, DensityPacket.class, DensityPacket::encode, DensityPacket::decode, DensityPacket::handle);
        CHANNEL.registerMessage(1, FocusPacket.class, FocusPacket::encode, FocusPacket::decode, FocusPacket::handle);
        CHANNEL.registerMessage(2, RuneSyncPacket.class, RuneSyncPacket::encode, RuneSyncPacket::decode, RuneSyncPacket::handle);
        CHANNEL.registerMessage(3, RuneActionPacket.class, RuneActionPacket::encode, RuneActionPacket::decode, RuneActionPacket::handle);
    }
}
