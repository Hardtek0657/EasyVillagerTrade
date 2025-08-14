package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.EasyVillagerTrade;
import de.gamedude.easyvillagertrade.screen.TradeSelectScreen;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import de.gamedude.easyvillagertrade.utils.TradingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EasyVillagerTradeBase {

    private TradingState state;
    private final TradeRequestContainer tradeRequestContainer;
    private final SelectionInterface selectionInterface;
    private final TradeRequestInputHandler tradeRequestInputHandler;
    private final TradeInterface tradeInterface;
    private final Minecraft minecraft;
    private boolean villagerSleeping = false;

    public EasyVillagerTradeBase() {
        this.minecraft = Minecraft.getInstance();
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
        return selectionInterface.isVillagerSleeping();
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
        if (state == TradingState.INACTIVE || selectionInterface.isVillagerSleeping())
            return;

        switch (state) {
            case BREAK_WORKSTATION -> handleBreak();
            case PLACE_WORKSTATION -> handlePlacement();
            case SELECT_TRADE -> tradeInterface.selectTrade();
            case APPLY_TRADE -> tradeInterface.applyTrade();
            case PICKUP_TRADE -> tradeInterface.pickupBook();
            case WAIT_JOB_LOSS -> {
                if (selectionInterface.getVillager().getVillagerData().getProfession() == VillagerProfession.NONE)
                    setState(TradingState.PLACE_WORKSTATION);
            }
            case WAIT_PROFESSION -> {
                if (selectionInterface.getVillager().getVillagerData().getProfession() != VillagerProfession.NONE)
                    setState(TradingState.CHECK_OFFERS);
            }
            case CHECK_OFFERS -> {
                // Try to interact with villager to get trade offers
                handleInteractionWithVillager();
            }
        }
    }

    private void handlePlacement() {
        LocalPlayer player = minecraft.player;
        BlockPos lecternPos = selectionInterface.getLecternPos();

        if (player.getOffhandItem().equals(ItemStack.EMPTY)) {
            player.sendSystemMessage(Component.translatable("evt.logic.lectern_non"));
            setState(TradingState.INACTIVE);
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(
            Vec3.atBottomCenterOf(lecternPos).add(0, 1, 0),
            Direction.UP,
            lecternPos,
            false
        );
        minecraft.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hitResult);
        player.swing(InteractionHand.OFF_HAND);

        setState(TradingState.WAIT_PROFESSION);
    }

    private void handleBreak() {
        Level world = minecraft.level;
        LocalPlayer player = minecraft.player;
        BlockPos blockPos = selectionInterface.getLecternPos();

        if (world == null || player == null) return;

        int preventionValue = EasyVillagerTrade.CONFIG.getProperty("preventAxeBreakingValue").getAsInt();
        ItemStack tool = player.getMainHandItem();
        if (preventionValue != -1) {
            if (tool.getMaxDamage() - tool.getDamageValue() <= preventionValue) {
                player.sendSystemMessage(Component.translatable("evt.logic.axe_durability"));
                setState(TradingState.INACTIVE);
                return;
            }
        }

        if (blockPos == null) {
            player.sendSystemMessage(Component.translatable("evt.logic.pos_not_set"));
            setState(TradingState.INACTIVE);
            return;
        }

        if (world.getBlockState(selectionInterface.getLecternPos()).getBlock() == Blocks.LECTERN) {
            minecraft.gameMode.continueDestroyBlock(selectionInterface.getLecternPos(), Direction.UP);
            player.swing(InteractionHand.MAIN_HAND, true);
            player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        } else {
            setState(TradingState.WAIT_JOB_LOSS);
        }
    }

    public void checkVillagerOffers(MerchantOffers tradeOffers) {
        // Use centralized sleep state check first
        if (selectionInterface.isVillagerSleeping()) {
            TradeSelectScreen.addLogMessage("Villager is sleeping - skipping offer check");
            return;
        }

        // Validate critical objects
        if (minecraft == null || minecraft.player == null || tradeRequestContainer == null ||
            tradeInterface == null || selectionInterface == null) {
            System.err.println("[EVT] Critical objects not initialized");
            return;
        }

        System.out.println("[EVT] Scanning all book offers...");

        System.out.println("[EVT] TradeRequests in Container:");
        for (TradeRequest request : tradeRequestContainer.getTradeRequests()) {
            if (request != null && request.enchantment() != null) {
                System.out.printf(
                    "[EVT] - %s (Lvl %d, Max Price: %d)%n",
                    request.enchantment().value().getFullname(request.level()).getString(),
                    request.level(),
                    request.maxPrice()
                );
            }
        }

        // Phase 1: Collect valid offers with enhanced null checks
        List<MerchantOffer> validBookOffers = new ArrayList<>();
        for (MerchantOffer offer : tradeOffers) {
            try {
                if (offer == null || offer.getResult().isEmpty() ||
                    offer.getResult().getItem() != Items.ENCHANTED_BOOK) {
                    continue;
                }

                // Log NBT data of the book
                System.out.println("[EVT] NBT Data of Book: " + offer.getResult().getTag());

                Map<Enchantment, Integer> enchantmentMap = EnchantmentHelper.getEnchantments(offer.getResult());
                if (enchantmentMap == null || enchantmentMap.isEmpty()) {
                    continue;
                }

                Enchantment enchantment = enchantmentMap.keySet().iterator().next();
                if (enchantment == null || BuiltInRegistries.ENCHANTMENT.getKey(enchantment) == null) {
                    System.out.println("[EVT] Skipping offer with invalid enchantment");
                    continue;
                }

                validBookOffers.add(offer);
            } catch (Exception e) {
                System.err.println("[EVT] Error processing offer: " + e.getMessage());
            }
        }

        if (validBookOffers.isEmpty()) {
            System.out.println("[EVT] No valid enchanted books found");
            setState(TradingState.BREAK_WORKSTATION);
            return;
        }

        // Phase 2: Find best matching offer with thread-safe iteration
        MerchantOffer bestOffer = null;
        TradeRequest bestRequest = null;

        for (MerchantOffer offer : validBookOffers) {
            try {
                Map<Enchantment, Integer> enchantmentMap = EnchantmentHelper.getEnchantments(offer.getResult());
                Enchantment enchantment = enchantmentMap.keySet().iterator().next();
                int level = enchantmentMap.get(enchantment);
                int price = offer.getCostA().getCount();

                System.out.printf(
                    "[EVT] Checking offer: %s (Lvl %d) for %d emeralds%n",
                    enchantment.getFullname(level).getString(),
                    level,
                    price
                );
                TradeSelectScreen.addLogMessage(
                    String.format(
                        "Checking offer: %s (Lvl %d) for %d emeralds",
                        enchantment.getFullname(level).getString(),
                        level,
                        price
                    )
                );

                TradeRequest villagerOffer = new TradeRequest(
                    BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment),
                    level,
                    price
                );

                // Thread-safe matching against container
                for (TradeRequest request : tradeRequestContainer.getTradeRequests()) {
                    if (request != null && request.enchantment() != null && request.matches(villagerOffer)) {
                        System.out.printf(
                            "[EVT] Comparing: Requested=%s (ID: %s, Lvl %d), Offered=%s (ID: %s, Lvl %d)%n",
                            request.enchantment().value().getFullname(request.level()).getString(),
                            BuiltInRegistries.ENCHANTMENT.getKey(request.enchantment().value()),
                            request.level(),
                            enchantment.getFullname(level).getString(),
                            BuiltInRegistries.ENCHANTMENT.getKey(enchantment),
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
                System.err.println("[EVT] Error evaluating offer: " + e.getMessage());
            }
        }

        // Phase 3: Handle results with state validation
        if (bestOffer != null && bestRequest != null) {
            try {
                Map<Enchantment, Integer> bestEnchantmentMap = EnchantmentHelper.getEnchantments(bestOffer.getResult());
                Enchantment bestEnchantment = bestEnchantmentMap.keySet().iterator().next();
                int bestLevel = bestEnchantmentMap.get(bestEnchantment);

                System.out.printf(
                    "[EVT] Final trade match: %s (Lvl %d) for %d emeralds%n",
                    bestEnchantment.getFullname(bestLevel).getString(),
                    bestLevel,
                    bestOffer.getCostA().getCount()
                );

                // Feedback
                minecraft.player.sendSystemMessage(
                    Component.translatable(
                        "evt.logic.trade_found",
                        "§e" + bestEnchantment.getFullname(bestLevel).getString(),
                        "§a" + bestRequest.maxPrice()
                    )
                );

                minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_BREAK, 2f)
                );

                // State transition
                if (!isVillagerSleeping()) {
                    // Double-check sleep state before proceeding
                    tradeRequestContainer.removeTradeRequestByEnchantment(
                        BuiltInRegistries.ENCHANTMENT.wrapAsHolder(bestEnchantment)
                    );
                    tradeInterface.setTradeSlotID(tradeOffers.indexOf(bestOffer));
                    setState(TradingState.SELECT_TRADE);
                } else {
                    System.out.println("[EVT] Villager fell asleep during trade - aborting");
                    setState(TradingState.INACTIVE);
                }
            } catch (Exception e) {
                System.err.println("[EVT] Critical error finalizing trade: " + e.getMessage());
                setState(TradingState.BREAK_WORKSTATION);
            }
        } else {
            System.out.println("[EVT] No matching trades found");
            setState(TradingState.BREAK_WORKSTATION);
        }
    }

    public void handleInteractionWithVillager() {
        minecraft.gameMode.interact(minecraft.player, selectionInterface.getVillager(), InteractionHand.MAIN_HAND);
    }
}
