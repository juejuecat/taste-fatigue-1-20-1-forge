package com.juejuecat.taste_fatigue;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Optional;
import java.util.function.Supplier;

import static com.juejuecat.taste_fatigue.TasteFatigue.MODID;

public class TFNetwork {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(MODID, "sync_use_duration"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    public static void register() {
        INSTANCE.registerMessage(
                1,
                SyncUseDurationMsg.class,
                SyncUseDurationMsg::encode,
                SyncUseDurationMsg::decode,
                SyncUseDurationMsg::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)   // 必须是 Optional
        );
    }

    public static void sendToPlayer(ServerPlayer player, int duration) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SyncUseDurationMsg(duration));
    }

    public static class SyncUseDurationMsg {
        private final int duration;

        public SyncUseDurationMsg(int duration) {
            this.duration = duration;
        }

        public static void encode(SyncUseDurationMsg msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.duration);
        }

        public static SyncUseDurationMsg decode(FriendlyByteBuf buf) {
            return new SyncUseDurationMsg(buf.readInt());
        }

        public static void handle(SyncUseDurationMsg msg, Supplier<NetworkEvent.Context> ctxSup) {
            NetworkEvent.Context ctx = ctxSup.get();
            ctx.enqueueWork(() -> {
                TFClientEvents.setPendingDuration(msg.duration);
            });
            ctx.setPacketHandled(true);
        }
    }
}
