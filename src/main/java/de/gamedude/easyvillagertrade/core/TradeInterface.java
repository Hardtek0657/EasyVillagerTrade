package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.utils.TradingState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class TradeInterface {

    private final EasyVillagerTradeBase modBase;
    private final Minecraft minecraft;
    private int tradeSlotID;

    public TradeInterface(EasyVillagerTradeBase modBase) {
        this.modBase = modBase;
        this.minecraft = Minecraft.getInstance();
    }

    public void setTradeSlotID(int tradeSlotID) {
        this.tradeSlotID = tradeSlotID;
    }

    public void selectTrade() {
        if (modBase.getSelectionInterface().isVillagerSleeping()) {
            System.out.println("[EVT] Cannot trade - villager is sleeping");
            return;
        }
        modBase.handleInteractionWithVillager();
        minecraft.getConnection().send(new ServerboundSelectTradePacket(tradeSlotID));

        modBase.setState(TradingState.APPLY_TRADE);
    }

    public void applyTrade() {
        AbstractContainerMenu currentScreenHandler = minecraft.player.containerMenu;
        minecraft.gameMode.handleInventoryMouseClick(currentScreenHandler.containerId, 2, 0, ClickType.PICKUP, minecraft.player);

        modBase.setState(TradingState.PICKUP_TRADE);
    }

    public void pickupBook() {
        Player player = minecraft.player;
        AbstractContainerMenu currentScreenHandler = player.containerMenu;
        int freeSlot = getFreeSlot();

        if (0 <= freeSlot && freeSlot <= 8)
            freeSlot += 30;
        else if (freeSlot != -999)
            freeSlot -= 6;
        else
            player.sendSystemMessage(Component.translatable("evt.logic.book_drop"));

        minecraft.gameMode.handleInventoryMouseClick(currentScreenHandler.containerId, freeSlot, 0, ClickType.PICKUP, minecraft.player);
        modBase.setState(TradingState.INACTIVE);
    }

    private int getFreeSlot() {
        NonNullList<ItemStack> list = minecraft.player.getInventory().items;

        long sumOfEmpty = list.stream().filter(ItemStack::isEmpty).count();
        if (sumOfEmpty <= 2)
            return -999;
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).isEmpty())
                return i;
        }
        return -999;
    }
}
