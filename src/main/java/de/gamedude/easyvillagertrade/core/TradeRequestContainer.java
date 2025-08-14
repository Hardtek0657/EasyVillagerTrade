package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.screen.TradeSelectScreen;
import de.gamedude.easyvillagertrade.utils.ModConfig;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

public class TradeRequestContainer {

    private final Set<TradeRequest> tradeRequestSet;
    private final ModConfig config;

    public TradeRequestContainer() {
        this.config = ModConfig.load();
        this.tradeRequestSet = new CopyOnWriteArraySet<>();

        if (config.tradeRequests != null) {
            config.tradeRequests
                .stream()
                .filter(Objects::nonNull)
                .filter(request -> request.enchantment() != null)
                .forEach(tradeRequestSet::add);
        }
    }

    public void addTradeRequest(TradeRequest tradeRequest) {
        if (tradeRequest != null && tradeRequest.enchantment() != null) {
            this.tradeRequestSet.add(tradeRequest);
            saveToConfig();
        }
    }

    public void removeTradeRequestByEnchantment(Holder<Enchantment> enchantment) {
        if (enchantment == null || enchantment.value() == null) {
            System.err.println("[EVT] Attempted to remove null enchantment");
            return;
        }

        boolean removed = this.tradeRequestSet.removeIf(
            request ->
                request != null &&
                request.enchantment() != null &&
                BuiltInRegistries.ENCHANTMENT.getKey(request.enchantment().value()).equals(
                    BuiltInRegistries.ENCHANTMENT.getKey(enchantment.value())
                )
        );

        if (removed) {
            System.out.println(
                "[EVT] Removed trades for " + enchantment.value().getFullname(1).getString()
            );
            saveToConfig();
        }
    }

    public void removeTradeRequestByEnchantment(Enchantment enchantment) {
        if (enchantment == null) {
            System.err.println("[EVT] Attempted to remove null enchantment");
            return;
        }

        boolean removed = this.tradeRequestSet.removeIf(
            request ->
                request != null &&
                request.enchantment() != null &&
                BuiltInRegistries.ENCHANTMENT.getKey(request.enchantment().value()).equals(
                    BuiltInRegistries.ENCHANTMENT.getKey(enchantment)
                )
        );

        if (removed) {
            System.out.println(
                "[EVT] Removed trades for " + enchantment.getFullname(1).getString()
            );
            saveToConfig();
        }
    }

    public void removeTradeRequest(TradeRequest request) {
        if (request != null && this.tradeRequestSet.remove(request)) {
            saveToConfig();
        }
    }

    public Set<TradeRequest> getTradeRequests() {
        return Collections.unmodifiableSet(tradeRequestSet); // Return read-only view
    }

    public boolean matchesAny(TradeRequest offer) {
        if (offer == null || offer.enchantment() == null) {
            return false;
        }

        return tradeRequestSet
            .stream()
            .filter(Objects::nonNull)
            .filter(request -> request.enchantment() != null)
            .anyMatch(request -> request.matches(offer));
    }

    public void saveToConfig() {
        Minecraft.getInstance().execute(() -> {
            try {
                config.tradeRequests.clear();
                config.tradeRequests.addAll(tradeRequestSet);
                config.save();
                System.out.println(
                    "[EVT] Successfully saved " +
                    tradeRequestSet.size() +
                    " trade requests"
                );
            } catch (Exception e) {
                System.err.println(
                    "[EVT] Failed to save trade requests: " + e.getMessage()
                );
                TradeSelectScreen.addLogMessage("§cFailed to save config!");
            }
        });
    }
}
