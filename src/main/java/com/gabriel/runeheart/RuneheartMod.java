package com.gabriel.runeheart;

import net.minecraft.util.ResourceLocation;
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
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public RuneheartMod() {
        RuneConfig.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent e) -> BlockCloverBridge.logStatus());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> RuneCommand.register(e.getDispatcher()));
        CHANNEL.registerMessage(0, DensityPacket.class, DensityPacket::encode, DensityPacket::decode, DensityPacket::handle);
    }
}
