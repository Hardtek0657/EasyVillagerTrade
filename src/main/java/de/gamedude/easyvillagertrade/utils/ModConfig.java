package de.gamedude.easyvillagertrade.utils;

import com.google.gson.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class ModConfig {

    private static final Path CONFIG_PATH = Minecraft.getInstance()
        .gameDirectory
        .toPath()
        .resolve("config")
        .resolve("easyvillagertrade.json");

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Enchantment.class, new EnchantmentSerializer())
        .create();

    public List<TradeRequest> tradeRequests = new ArrayList<>();

    public static ModConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return GSON.fromJson(
                    Files.readString(CONFIG_PATH),
                    ModConfig.class
                );
            }
        } catch (IOException e) {
            System.err.println("Failed to load config:");
            e.printStackTrace();
        }
        return new ModConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("Failed to save config:");
            e.printStackTrace();
        }
    }

    private static class EnchantmentSerializer
        implements JsonSerializer<Enchantment>, JsonDeserializer<Enchantment> {

        @Override
        public JsonElement serialize(
            Enchantment src,
            Type typeOfSrc,
            JsonSerializationContext context
        ) {
            return new JsonPrimitive(
                BuiltInRegistries.ENCHANTMENT.getKey(src).toString()
            );
        }

        @Override
        public Enchantment deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            ResourceLocation id = new ResourceLocation(json.getAsString());
            Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
            if (enchantment == null) {
                System.err.println(
                    "[EVT] WARNING: Enchantment not found: " + id
                );
            }
            return enchantment; // Returns null if missing
        }
    }
}
