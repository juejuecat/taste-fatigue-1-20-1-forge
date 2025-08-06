package com.juejuecat.taste_fatigue;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(TasteFatigue.MODID)
public class TasteFatigue {
    public static final String MODID = "taste_fatigue";
    public TasteFatigue() {
        TasteFatigueConfig.load();
        TFNetwork.register(); // 注册网络
        MinecraftForge.EVENT_BUS.register(new TasteFatigueEvents());
    }
}
