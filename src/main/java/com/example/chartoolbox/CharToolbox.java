package com.example.chartoolbox;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Char's Toolbox: one file that carries Char's handy mods inside it and gives each part an on/off switch.
 * It has no network channel of its own, so it can never be the reason a PC is refused by a server.
 */
@Mod(CharToolbox.MODID)
public class CharToolbox {
    public static final String MODID = "chartoolbox";

    public CharToolbox() {
        Switches.load();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.example.chartoolbox.client.ToolboxClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
