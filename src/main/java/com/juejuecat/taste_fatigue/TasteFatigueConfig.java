package com.juejuecat.taste_fatigue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TasteFatigueConfig {
    public static Config CONFIG;

    public static class Config {
        public Stage[] stages;
    }

    public static class Stage {
        public int maxDisgust;
        public String emoji;
        public double durationMult;
        public Effect[] effects;

        public Stage(int maxDisgust, String emoji, double durationMult, Effect[] effects) {
            this.maxDisgust = maxDisgust;
            this.emoji = emoji;
            this.durationMult = durationMult;
            this.effects = effects;
        }
    }

    public static class Effect {
        public String type;
        public float amount;
        public int duration;
        public String potion;

        public Effect(String type, float amount, int duration, String potion) {
            this.type = type;
            this.amount = amount;
            this.duration = duration;
            this.potion = potion;
        }
    }

    public static void load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("taste_fatigue.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.notExists(path)) {
                String defaultJson = gson.toJson(defaultConfig());
                Files.writeString(path, defaultJson);
            }
            CONFIG = gson.fromJson(Files.readString(path), Config.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Config defaultConfig() {
        Config config = new Config();
        config.stages = new Stage[]{
                new Stage(10, "(^.^)", 0.8, new Effect[]{new Effect("heal", 2, 0, null)}),
                new Stage(30, "(-.-)", 1.0, new Effect[]{}),
                new Stage(40, "(o.O)", 1.5, new Effect[]{new Effect("potion", 0, 100, "minecraft:nausea")}),
                new Stage(50, "(T-T)", 2.0, new Effect[]{new Effect("hurt", 2, 0, null), new Effect("potion", 0, 600, "minecraft:nausea")}),
                new Stage(Integer.MAX_VALUE, "(x.x)", 3.0, new Effect[]{new Effect("hurt", 6, 0, null), new Effect("potion", 0, 1200, "minecraft:nausea")})
        };
        return config;
    }
}
