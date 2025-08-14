package de.gamedude.easyvillagertrade.core;

import static de.gamedude.easyvillagertrade.EasyVillagerTrade.modBase;

import de.gamedude.easyvillagertrade.screen.TradeSelectScreen;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import de.gamedude.easyvillagertrade.utils.TradingState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

public class EasyVillagerTradeBase {

    private TradingState state;
    private final TradeRequestContainer tradeRequestContainer;
    private final SelectionInterface selectionInterface;
    private final TradeRequestInputHandler tradeRequestInputHandler;
    private final TradeInterface tradeInterface;
    private final MinecraftClient minecraftClient;
    private boolean villagerSleeping = false;

    public EasyVillagerTradeBase() {
        this.minecraftClient = MinecraftClient.getInstance();
        this.tradeRequestContainer = new TradeRequestContainer();
        this.selectionInterface = new SelectionInterface(this);
        this.tradeRequestInputHandler = new TradeRequestInputHandler();
        this.tradeInterface = new TradeInterface(this);
        this.state = TradingState.INACTIVE;
    }

    public TradeRequestInputHandler getTradeRequestInputHandler() {
        return tradeRequestInputHandler;
    }

    public SelectionInterface getSelectionInterface() {
        return selectionInterface;
    }

    public TradeRequestContainer getTradeRequestContainer() {
        return tradeRequestContainer;
    }

    public void setState(TradingState state) {
        this.state = state;
    }

    public TradingState getState() {
        return state;
    }

    public boolean isVillagerSleeping() {
        return selectionInterface.isVillagerSleeping(); // Delegate to selection interface
    }

    public void setVillagerSleeping(boolean sleeping) {
        if (this.villagerSleeping != sleeping) {
            this.villagerSleeping = sleeping;
            String message = sleeping
                ? "Villager fell asleep - pausing trades"
                : "Villager woke up - resuming trades";
            TradeSelectScreen.addLogMessage(message);
        }
    }

    public void handle() {
        if (
            state == TradingState.INACTIVE ||
            selectionInterface.isVillagerSleeping()
        ) return;

        switch (state) {
            case BREAK_WORKSTATION -> handleBreak();
            case PLACE_WORKSTATION -> handlePlacement();
            case SELECT_TRADE -> tradeInterface.selectTrade();
            case APPLY_TRADE -> tradeInterface.applyTrade();
            case PICKUP_TRADE -> tradeInterface.pickupBook();
            case WAIT_JOB_LOSS -> {
                if (
                    selectionInterface
                        .getVillager()
                        .getVillagerData()
                        .getProfession() ==
                    VillagerProfession.NONE
                ) setState(TradingState.PLACE_WORKSTATION);
            }
        }
    }

    private void handlePlacement() {
        ClientPlayerEntity player = minecraftClient.player;
        BlockPos lecternPos = selectionInterface.getLecternPos();

        if (player.getOffHandStack().equals(ItemStack.EMPTY)) {
            player.sendMessage(Text.translatable("evt.logic.lectern_non"));
            setState(TradingState.INACTIVE);
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(
            new Vec3d(lecternPos.getX(), lecternPos.getY(), lecternPos.getZ()),
            Direction.UP,
            lecternPos,
            false
        );
        minecraftClient.interactionManager.interactBlock(
            player,
            Hand.OFF_HAND,
            hitResult
        );
        minecraftClient
            .getNetworkHandler()
            .sendPacket(new HandSwingC2SPacket(Hand.OFF_HAND));

        setState(TradingState.WAIT_PROFESSION);
    }

    private void handleBreak() {
        World world = minecraftClient.world;
        ClientPlayerEntity player = minecraftClient.player;
        BlockPos blockPos = selectionInterface.getLecternPos();

        if (world == null || player == null) return;
        ItemStack axe = player.getMainHandStack();
        if (axe.getMaxDamage() - axe.getDamage() < 20) {
            player.sendMessage(Text.translatable("evt.logic.axe_durability"));
            setState(TradingState.INACTIVE);
            return;
        }

        if (blockPos == null) {
            player.sendMessage(Text.translatable("evt.logic.pos_not_set"));
            setState(TradingState.INACTIVE);
            return;
        }

        if (
            world
                .getBlockState(selectionInterface.getLecternPos())
                .getBlock() ==
            Blocks.LECTERN
        ) {
            minecraftClient.interactionManager.updateBlockBreakingProgress(
                selectionInterface.getLecternPos(),
                Direction.UP
            );
            player.swingHand(Hand.MAIN_HAND, true);
            player.networkHandler.sendPacket(
                new HandSwingC2SPacket(Hand.MAIN_HAND)
            );
        } else {
            state = TradingState.WAIT_JOB_LOSS;
        }
    }

    public void checkVillagerOffers(TradeOfferList tradeOffers) {
        // Use centralized sleep state check first
        if (selectionInterface.isVillagerSleeping()) {
            // Use selection interface
            TradeSelectScreen.addLogMessage(
                "Villager is sleeping - skipping offer check"
            );
            return;
        }

        // Validate critical objects
        if (
            minecraftClient == null ||
            minecraftClient.player == null ||
            tradeRequestContainer == null ||
            tradeInterface == null ||
            selectionInterface == null
        ) {
            System.err.println("[EVT] Critical objects not initialized");
            return;
        }

        System.out.println("[EVT] Scanning all book offers...");

        System.out.println("[EVT] TradeRequests in Container:");
        for (TradeRequest request : tradeRequestContainer.getTradeRequests()) {
            if (request != null && request.enchantment() != null) {
                System.out.printf(
                    "[EVT] - %s (Lvl %d, Max Price: %d)%n",
                    request.enchantment().getName(request.level()).getString(),
                    request.level(),
                    request.maxPrice()
                );
            }
        }

        // Phase 1: Collect valid offers with enhanced null checks
        List<TradeOffer> validBookOffers = new ArrayList<>();
        for (TradeOffer offer : tradeOffers) {
            try {
                if (
                    offer == null ||
                    offer.getSellItem().isEmpty() ||
                    offer.getSellItem().getItem() != Items.ENCHANTED_BOOK
                ) {
                    continue;
                }

                // Log NBT data of the book
                System.out.println(
                    "[EVT] NBT Data of Book: " + offer.getSellItem().getNbt()
                );

                Map<Enchantment, Integer> enchantmentMap =
                    EnchantmentHelper.get(offer.getSellItem());
                if (enchantmentMap == null || enchantmentMap.isEmpty()) {
                    continue;
                }

                Enchantment enchantment = enchantmentMap
                    .keySet()
                    .iterator()
                    .next();
                if (
                    enchantment == null ||
                    Registries.ENCHANTMENT.getId(enchantment) == null
                ) {
                    System.out.println(
                        "[EVT] Skipping offer with invalid enchantment"
                    );
                    continue;
                }

                validBookOffers.add(offer);
            } catch (Exception e) {
                System.err.println(
                    "[EVT] Error processing offer: " + e.getMessage()
                );
            }
        }

        if (validBookOffers.isEmpty()) {
            System.out.println("[EVT] No valid enchanted books found");
            setState(TradingState.BREAK_WORKSTATION);
            return;
        }

        // Phase 2: Find best matching offer with thread-safe iteration
        TradeOffer bestOffer = null;
        TradeRequest bestRequest = null;

        for (TradeOffer offer : validBookOffers) {
            try {
                Map<Enchantment, Integer> enchantmentMap =
                    EnchantmentHelper.get(offer.getSellItem());
                Enchantment enchantment = enchantmentMap
                    .keySet()
                    .iterator()
                    .next();
                int level = enchantmentMap.get(enchantment);
                int price = offer.getAdjustedFirstBuyItem().getCount();

                System.out.printf(
                    "[EVT] Checking offer: %s (Lvl %d) for %d emeralds%n",
                    enchantment.getName(level).getString(),
                    level,
                    price
                );
                TradeSelectScreen.addLogMessage(
                    String.format(
                        "Checking offer: %s (Lvl %d) for %d emeralds",
                        enchantment.getName(level).getString(),
                        level,
                        price
                    )
                );

                TradeRequest villagerOffer = new TradeRequest(
                    enchantment,
                    level,
                    price
                );

                // Thread-safe matching against container
                for (TradeRequest request : tradeRequestContainer.getTradeRequests()) {
                    if (
                        request != null &&
                        request.enchantment() != null &&
                        request.matches(villagerOffer)
                    ) {
                        System.out.printf(
                            "[EVT] Comparing: Requested=%s (ID: %s, Lvl %d), Offered=%s (ID: %s, Lvl %d)%n",
                            request
                                .enchantment()
                                .getName(request.level())
                                .getString(),
                            Registries.ENCHANTMENT.getId(request.enchantment()),
                            request.level(),
                            enchantment.getName(level).getString(),
                            Registries.ENCHANTMENT.getId(enchantment),
                            level
                        );

                        if (bestOffer == null || level > bestRequest.level()) {
                            bestOffer = offer;
                            bestRequest = request;
                            System.out.println("[EVT] New best match found");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(
                    "[EVT] Error evaluating offer: " + e.getMessage()
                );
            }
        }

        // Phase 3: Handle results with state validation
        if (bestOffer != null && bestRequest != null) {
            try {
                Map<Enchantment, Integer> bestEnchantmentMap =
                    EnchantmentHelper.get(bestOffer.getSellItem());
                Enchantment bestEnchantment = bestEnchantmentMap
                    .keySet()
                    .iterator()
                    .next();
                int bestLevel = bestEnchantmentMap.get(bestEnchantment);

                System.out.printf(
                    "[EVT] Final trade match: %s (Lvl %d) for %d emeralds%n",
                    bestEnchantment.getName(bestLevel).getString(),
                    bestLevel,
                    bestOffer.getAdjustedFirstBuyItem().getCount()
                );

                // Feedback
                minecraftClient.player.sendMessage(
                    Text.translatable(
                        "evt.logic.trade_found",
                        "§e" + bestEnchantment.getName(bestLevel).getString(),
                        "§a" + bestRequest.maxPrice()
                    )
                );

                minecraftClient
                    .getSoundManager()
                    .play(
                        PositionedSoundInstance.master(
                            SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK,
                            2f,
                            1f
                        )
                    );

                // State transition
                if (!isVillagerSleeping()) {
                    // Double-check sleep state before proceeding
                    tradeRequestContainer.removeTradeRequestByEnchantment(
                        bestEnchantment
                    );
                    tradeInterface.setTradeSlotID(
                        tradeOffers.indexOf(bestOffer)
                    );
                    setState(TradingState.SELECT_TRADE);
                } else {
                    System.out.println(
                        "[EVT] Villager fell asleep during trade - aborting"
                    );
                    setState(TradingState.INACTIVE);
                }
            } catch (Exception e) {
                System.err.println(
                    "[EVT] Critical error finalizing trade: " + e.getMessage()
                );
                setState(TradingState.BREAK_WORKSTATION);
            }
        } else {
            System.out.println("[EVT] No matching trades found");
            setState(TradingState.BREAK_WORKSTATION);
        }
    }

    public void handleInteractionWithVillager() {
        minecraftClient.interactionManager.interactEntity(
            minecraftClient.player,
            selectionInterface.getVillager(),
            Hand.MAIN_HAND
        );
    }
}
