package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.utils.TradeRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;

public class TradeRequestInputHandler {

    public TradeRequest parseCommandInput(Enchantment enchantment, int inputLevel, int maxPrice) {
        int level = mapLevel(enchantment, inputLevel);
        int price = mapPrice(maxPrice);
        return new TradeRequest(BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment), level, price);
    }

    public int handleInputUI(String enchantmentInput, String levelInput, String priceInput, java.util.function.Consumer<TradeRequest> tradeRequestConsumer) {
        Holder<Enchantment> enchantmentEntry = getEnchantment(enchantmentInput);
        if(enchantmentEntry == null)
            return 1; // no valid enchantment
        if(notInt(priceInput))
            return 2; // no valid price
        Enchantment enchantment = enchantmentEntry.value();
        int price = Mth.clamp(Integer.parseInt(priceInput), 1, 64);

        if(levelInput.equals("*")) { // add all possible levels
            for(int levelIterator = 1; levelIterator <= enchantment.getMaxLevel(); levelIterator++) {
                TradeRequest request = new TradeRequest(enchantmentEntry, levelIterator, price);
                tradeRequestConsumer.accept(request);
            }
            return 0;
        }
        if(notInt(levelInput))
            return 3; // no valid level
        int level = Mth.clamp(Integer.parseInt(levelInput), 1, enchantment.getMaxLevel());

        TradeRequest request = new TradeRequest(enchantmentEntry, level, price);
        tradeRequestConsumer.accept(request);
        return 0;
    }

    public TradeRequest handleGUIInput(String enchantmentInput, String levelInput, String priceInput) {
        Holder<Enchantment> enchantmentHolder = getEnchantment(enchantmentInput);
        if (enchantmentHolder == null) return null;

        Enchantment enchantment = enchantmentHolder.value();
        int level = isInteger(levelInput) ? mapLevel(enchantment, Integer.parseInt(levelInput)) : -1;
        int maxPrice = isInteger(priceInput) ? mapPrice(Integer.parseInt(priceInput)) : -1;

        if (level == -1 || maxPrice == -1) return null;
        return new TradeRequest(enchantmentHolder, level, maxPrice);
    }

    public Holder<Enchantment> getEnchantment(String enchantmentInput) {
        return BuiltInRegistries.ENCHANTMENT.stream()
            .filter(enchantment ->
                enchantment.getFullname(1).getString().toLowerCase().contains(enchantmentInput.trim().toLowerCase()) ||
                enchantment.getDescriptionId().toLowerCase().contains(enchantmentInput.trim().toLowerCase()))
            .findFirst()
            .map(BuiltInRegistries.ENCHANTMENT::wrapAsHolder)
            .orElse(null);
    }

    private int mapPrice(int maxPriceInput) {
        return Mth.clamp(maxPriceInput, 1, 64);
    }

    private int mapLevel(Enchantment enchantment, int inputLevel) {
        return Mth.clamp(inputLevel, 1, enchantment.getMaxLevel());
    }

    private boolean notInt(String tryParse) {
        try {
            Integer.parseInt(tryParse);
        } catch (NumberFormatException e) {
            return true;
        }
        return false;
    }

    private boolean isInteger(String tryParse) {
        try {
            Integer.parseInt(tryParse);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
