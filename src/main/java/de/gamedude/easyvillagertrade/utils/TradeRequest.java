package de.gamedude.easyvillagertrade.utils;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record TradeRequest(Enchantment enchantment, int level, int maxPrice) {
    public boolean matches(TradeRequest other) {
        if (this.enchantment == null || other.enchantment() == null) {
            System.out.println("[EVT] Skipping null enchantment comparison");
            return false;
        }

        Identifier thisId = Registries.ENCHANTMENT.getId(this.enchantment);
        Identifier otherId = Registries.ENCHANTMENT.getId(other.enchantment());

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
