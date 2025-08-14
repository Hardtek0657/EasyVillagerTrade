package de.gamedude.easyvillagertrade.screen;

import de.gamedude.easyvillagertrade.EasyVillagerTrade;
import de.gamedude.easyvillagertrade.core.EasyVillagerTradeBase;
import de.gamedude.easyvillagertrade.screen.widget.EnchantmentInputWidget;
import de.gamedude.easyvillagertrade.screen.widget.TradeRequestListWidget;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TradeSelectScreen extends Screen {

    private final EasyVillagerTradeBase modBase = EasyVillagerTrade.getModBase();
    private static TradeSelectScreen currentInstance;
    private static final List<String> logMessages = new ArrayList<>();
    private final Font font;
    private final int enchantmentWidth;
    private final int levelWidth;
    private final int priceWidth;
    public final int widgetWidth;

    public TradeSelectScreen() {
        super(Component.empty());
        this.font = Minecraft.getInstance().font;
        this.enchantmentWidth = font.width("Enchantment");
        this.levelWidth = font.width("Level");
        this.priceWidth = font.width("Price");
        this.widgetWidth = priceWidth + levelWidth + enchantmentWidth + 50;
        currentInstance = this;
    }

    public static void addLogMessage(String message) {
        logMessages.add(message);
        if (logMessages.size() > 20) {
            logMessages.remove(0);
        }
        if (currentInstance != null) {
            currentInstance.updateLogDisplay();
        }
    }

    private void updateLogDisplay() {
        // Forces the screen to redraw
        Minecraft.getInstance().setScreen(this);
    }

    @Override
    protected void init() {
        int px = (int) (this.width / 50f);
        int x = this.width - widgetWidth - px;

        EnchantmentInputWidget enchantmentInputWidget = new EnchantmentInputWidget(x + 10, px + 15, enchantmentWidth, 20);

        EditBox levelTextFieldWidget = new EditBox(font, x + 20 + enchantmentWidth, px + 15, levelWidth, 20, Component.literal("Level"));
        EditBox priceTextFieldWidget = new EditBox(font, x + 30 + enchantmentWidth + levelWidth, px + 15, priceWidth, 20, Component.literal("Price"));

        TradeRequestListWidget tradeRequestListWidget = new TradeRequestListWidget(x + 10, px + 80, width - x - px - 20, this.height - px - 50);
        modBase.getTradeRequestContainer().getTradeRequests().stream()
            .filter(request -> request.enchantment() != null)
            .forEach(tradeRequestListWidget::addEntry);

        this.addRenderableWidget(enchantmentInputWidget);
        this.addRenderableWidget(levelTextFieldWidget);
        this.addRenderableWidget(priceTextFieldWidget);

        Button addButton = Button.builder(Component.literal("Add"), button -> {
            int result = modBase.getTradeRequestInputHandler().handleInputUI(enchantmentInputWidget.getValue(), levelTextFieldWidget.getValue(), priceTextFieldWidget.getValue(), tradeRequest -> {
                if (!modBase.getTradeRequestContainer().getTradeRequests().contains(tradeRequest)) {
                    tradeRequestListWidget.addEntry(tradeRequest);
                    modBase.getTradeRequestContainer().addTradeRequest(tradeRequest); // This will auto-save
                    clearTextFieldWidgets(enchantmentInputWidget, levelTextFieldWidget, priceTextFieldWidget);
                    TradeSelectScreen.addLogMessage("Added trade request: " + tradeRequest.enchantment().value().getFullname(tradeRequest.level()).getString());
                }
            });

            switch (result) {
                case 0 -> clearTextFieldWidgets(enchantmentInputWidget, levelTextFieldWidget, priceTextFieldWidget);
                case 1 -> enchantmentInputWidget.setTextColor(Color.RED.getRGB());
                case 2 -> priceTextFieldWidget.setTextColor(Color.RED.getRGB());
                case 3 -> levelTextFieldWidget.setTextColor(Color.RED.getRGB());
            }
        }).bounds(x + 9, px + 15 + 20 + 5, 50, 20).build();

        Button removeButton = Button.builder(Component.literal("Remove"), button -> {
            Holder<Enchantment> enchantment = modBase.getTradeRequestInputHandler().getEnchantment(enchantmentInputWidget.getValue());
            if (enchantment == null) {
                enchantmentInputWidget.setTextColor(0xFF0000);
                return;
            }

            boolean removed = false;
            for (Iterator<TradeRequestListWidget.TradeRequestEntry> it = tradeRequestListWidget.children().iterator(); it.hasNext();) {
                TradeRequestListWidget.TradeRequestEntry entry = it.next();
                if (entry.tradeRequest.enchantment().value().equals(enchantment.value())) {
                    modBase.getTradeRequestContainer().removeTradeRequest(entry.tradeRequest); // Auto-saves
                    it.remove();
                    removed = true;
                }
            }

            if (removed) {
                clearTextFieldWidgets(enchantmentInputWidget, levelTextFieldWidget, priceTextFieldWidget);
                TradeSelectScreen.addLogMessage("Removed trade request: " + enchantment.value().getFullname(1).getString());
            }
        }).bounds(x + 70, px + 40, 50, 20).build();

        this.addRenderableWidget(addButton);
        this.addRenderableWidget(removeButton);
        this.addRenderableWidget(tradeRequestListWidget);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        renderables.forEach(element -> {
            if (element instanceof EditBox editBox)
                editBox.setTextColor(0xE0E0E0);
        });
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        renderables.forEach(element -> {
            if (element instanceof EditBox editBox)
                editBox.setTextColor(0xE0E0E0);
        });
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        int px = (int) (this.width / 50f);
        int x = this.width - px - widgetWidth;

        // Render log messages on the left side of the screen
        int logX = px + 10;
        int logY = px + 40;
        for (int i = 0; i < logMessages.size(); i++) {
            guiGraphics.drawString(font, logMessages.get(i), logX, logY + (i * 10), 0xFFFFFF, false);
        }

        guiGraphics.drawString(font, "Enchantment", x + 10, px + 6, 0xE0E0E0, false);
        guiGraphics.drawString(font, "Level", x + 20 + enchantmentWidth, px + 6, 0xE0E0E0, false);
        guiGraphics.drawString(font, "Price", x + 30 + enchantmentWidth + priceWidth, px + 6, 0xE0E0E0, false);
    }

    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int px = (int) (this.width / 50f);
        int x = this.width - px - widgetWidth;
        guiGraphics.fill(x, px, this.width - px, this.height - px, 0x96070707);
    }

    public void clearTextFieldWidgets(EditBox... textFieldWidgets) {
        Arrays.stream(textFieldWidgets).forEach(textFieldWidget -> textFieldWidget.setValue(""));
    }
}
