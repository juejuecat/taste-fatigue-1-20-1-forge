package com.juejuecat.taste_fatigue;

import com.juejuecat.taste_fatigue.TFNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "taste_fatigue")
public class TFClientEvents {

    // 缓存服务器下发的值
    private static int pendingDuration = -1;

    // 由网络包调用
    public static void setPendingDuration(int d) {
        pendingDuration = d;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 如果正在吃东西，且服务器下发了新值，则覆盖
        if (pendingDuration >= 0 && player.isUsingItem()) {
            player.useItemRemaining = pendingDuration;
            pendingDuration = -1;
        }
    }
}
