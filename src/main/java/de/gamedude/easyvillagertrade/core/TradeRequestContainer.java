package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.screen.TradeSelectScreen;
import de.gamedude.easyvillagertrade.utils.ModConfig;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TradeRequestContainer {

    private final Set<TradeRequest> tradeRequestSet;
    private final ModConfig config;



    public TradeRequestContainer() {
        this.config = ModConfig.load();
        this.tradeRequestSet = new CopyOnWriteArraySet<>();

        if (config.tradeRequests != null) {
            config.tradeRequests
                .stream();
                config.tradeRequests.stream()
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

    public void removeTradeRequestByEnchantment(Enchantment enchantment) {
        if (enchantment == null) {
            System.err.println("[EVT] Attempted to remove null enchantment");
            return;
        }

        boolean removed = this.tradeRequestSet.removeIf(
            request ->
                request != null &&
                request.enchantment() != null &&
                Registries.ENCHANTMENT.getId(request.enchantment()).equals(
                    Registries.ENCHANTMENT.getId(enchantment)
                )
        );

        if (removed) {
            System.out.println(
                "[EVT] Removed trades for " + enchantment.getName(1).getString()
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
        MinecraftClient.getInstance().execute(() -> {
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
