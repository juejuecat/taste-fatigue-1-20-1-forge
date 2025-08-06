package com.juejuecat.taste_fatigue;

import com.juejuecat.taste_fatigue.TasteFatigueConfig.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TasteFatigue.MODID)
public class TasteFatigueEvents {

    private static int decayTimer = 0;
    private static final int MAX_DISGUST = 200;
    private static final int DISGUST_DECAY_TICKS = 20 * 60 * 10;

    private static Stage getStage(int disgust) {
        for (Stage s : TasteFatigueConfig.CONFIG.stages) {
            if (disgust <= s.maxDisgust) return s;
        }
        return TasteFatigueConfig.CONFIG.stages[TasteFatigueConfig.CONFIG.stages.length - 1];
    }

    @SubscribeEvent
    public static void onFoodUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = e.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null) return;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        int disgust = getDisgust(player, id);
        Stage stage = getStage(disgust);
        int newDuration = (int) (stack.getItem().getUseDuration(stack) * stage.durationMult);

        e.setDuration(newDuration);
        displayDisgustLevel(player, disgust);
        player.getPersistentData().putInt("CurrentFoodDuration", newDuration);
        TFNetwork.sendToPlayer(player, newDuration);
        player.getPersistentData().putInt("TF_ClientDur", newDuration);
    }

    @SubscribeEvent
    public static void onFoodUseStop(LivingEntityUseItemEvent.Stop e) {
        if (e.getEntity() instanceof Player p) p.getPersistentData().remove("CurrentFoodDuration");
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = e.getItem();
        if (stack.getItem().getFoodProperties(stack, player) == null) return;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        int newDisgust = incrementDisgust(player, id);
        applyDisgustEffects(player, newDisgust);
        player.stopUsingItem();
    }

    private static void displayDisgustLevel(Player player, int disgust) {
        Stage stage = getStage(disgust);
        player.displayClientMessage(
                Component.literal(disgust + " - " + stage.emoji)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)).withItalic(true)),
                true
        );
    }

    private static void applyDisgustEffects(Player player, int disgust) {
        if (player.level().isClientSide) return;
        Stage stage = getStage(disgust);
        for (Effect ef : stage.effects) {
            switch (ef.type) {
                case "heal" -> player.heal(ef.amount);
                case "hurt" -> player.hurt(player.damageSources().magic(), ef.amount);
                case "potion" -> {
                    var effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(ef.potion));
                    if (effect != null) {
                        player.addEffect(new MobEffectInstance(effect, ef.duration, (int) ef.amount));
                    }
                }
            }
        }
    }

    private static int getDisgust(Player player, ResourceLocation id) {
        return loadPlayerDisgust(player).getOrDefault(id, 0);
    }

    private static int incrementDisgust(Player player, ResourceLocation id) {
        Map<ResourceLocation, Integer> map = loadPlayerDisgust(player);
        int cur = map.getOrDefault(id, 0);
        int next = Math.min(cur + 1, MAX_DISGUST);
        map.put(id, next);
        savePlayerDisgust(player, map);
        return next;
    }

    private static Map<ResourceLocation, Integer> loadPlayerDisgust(Player player) {
        Map<ResourceLocation, Integer> map = new HashMap<>();
        CompoundTag root = player.getPersistentData();
        if (root.contains("TasteFatigueDisgust", Tag.TAG_LIST)) {
            ListTag list = root.getList("TasteFatigueDisgust", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                CompoundTag tag = (CompoundTag) t;
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("FoodId"));
                if (id != null) map.put(id, tag.getInt("Value"));
            }
        }
        return map;
    }

    private static void savePlayerDisgust(Player player, Map<ResourceLocation, Integer> map) {
        ListTag list = new ListTag();
        map.forEach((id, val) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("FoodId", id.toString());
            tag.putInt("Value", val);
            list.add(tag);
        });
        player.getPersistentData().put("TasteFatigueDisgust", list);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            if (++decayTimer >= DISGUST_DECAY_TICKS) {
                decayTimer = 0;
                e.getServer().getPlayerList().getPlayers().forEach(TasteFatigueEvents::decayPlayerDisgust);
            }
        }
    }

    private static void decayPlayerDisgust(Player player) {
        Map<ResourceLocation, Integer> map = loadPlayerDisgust(player);
        boolean changed = false;
        for (ResourceLocation key : map.keySet()) {
            int v = map.get(key);
            if (v > 0) {
                map.put(key, v - 1);
                changed = true;
            }
        }
        if (changed) savePlayerDisgust(player, map);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone e) {
        if (e.isWasDeath()) e.getEntity().getPersistentData().merge(e.getOriginal().getPersistentData());
    }
}
