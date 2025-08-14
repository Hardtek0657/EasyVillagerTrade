package de.gamedude.easyvillagertrade.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public record TradeRequest(Holder<Enchantment> enchantment, int level, int maxPrice) {

    public boolean matches(TradeRequest other) {
        if (this.enchantment == null || other.enchantment() == null) {
            System.out.println("[EVT] Skipping null enchantment comparison");
            return false;
        }

        ResourceLocation thisId = BuiltInRegistries.ENCHANTMENT.getKey(this.enchantment.value());
        ResourceLocation otherId = BuiltInRegistries.ENCHANTMENT.getKey(other.enchantment().value());

        // First try exact match, then fall back to path-only match
        boolean idMatch =
            thisId.equals(otherId) ||
            thisId.getPath().equals(otherId.getPath());

        boolean levelMatch = this.level <= other.level();
        boolean priceMatch = this.maxPrice >= other.maxPrice();

        System.out.printf(
            "[EVT] Matching - Full IDs: %s vs %s | Paths: %s vs %s | Level: %d<=%d | Price: %d>=%d%n",
            thisId,
            otherId,
            thisId.getPath(),
            otherId.getPath(),
            this.level,
            other.level(),
            this.maxPrice,
            other.maxPrice()
        );

        return idMatch && levelMatch && priceMatch;
    }

    @Override
    public String toString() {
        return (
            "TradeRequest{" +
            "enchantment=" +
            enchantment +
            ", level=" +
            level +
            ", maxPrice=" +
            maxPrice +
            '}'
        );
    }
}
