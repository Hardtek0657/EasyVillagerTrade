package de.gamedude.easyvillagertrade.screen.widget;

import de.gamedude.easyvillagertrade.EasyVillagerTrade;
import de.gamedude.easyvillagertrade.core.EasyVillagerTradeBase;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TradeRequestListWidget extends AbstractWidget {

    private static final int ENTRY_HEIGHT = 32;
    private static int ENTRIES_PER_PAGE;

    private double scrollAmount;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<TradeRequestEntry> children;
    private final EasyVillagerTradeBase modBase;

    public TradeRequestListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.children = new ArrayList<>();
        this.modBase = EasyVillagerTrade.getModBase();
    }

    public int getEntryCount() {
        return children.size();
    }

    public TradeRequestEntry getEntry(int index) {
        return this.children.get(index);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        ENTRIES_PER_PAGE = (int) Math.ceil((height - y + 5) / (ENTRY_HEIGHT + 5f) - 1);
        if (ENTRIES_PER_PAGE == 0)
            return;

        this.renderBackground(guiGraphics);

        for (int index = 0; index < Math.min(getEntryCount(), ENTRIES_PER_PAGE); ++index) {
            getEntry(index + getOffset()).render(guiGraphics, index, x, y + 1, width);
        }
    }

    private int getOffset() {
        int maxScroll = getMaxScroll();
        int currentScroll = (int) Math.abs(this.scrollAmount);
        return Math.min((maxScroll > 0) ? (int) Math.ceil(maxScroll / (ENTRY_HEIGHT + 5f)) : 0, (int) Math.ceil(currentScroll / (ENTRY_HEIGHT + 5f)));
    }

    protected int getMaxPosition() {
        return getEntryCount() * (ENTRY_HEIGHT + 5) - 5;
    }

    public int getMaxScroll() {
        return getMaxPosition() - (ENTRIES_PER_PAGE * (ENTRY_HEIGHT + 5));
    }

    public List<TradeRequestEntry> children() {
        return children;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        this.setScrollAmount(scrollAmount - (vertical * (ENTRY_HEIGHT + 5)));
        return true;
    }

    public void setScrollAmount(double amount) {
        this.scrollAmount = Mth.clamp(amount, 0.0, this.getMaxScroll());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return x <= mouseX && mouseX <= (x + width) && y <= mouseY && mouseY <= height;
    }

    private void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(x - 1, y, x + width + 1, y + 1, -1); // horizontal
        guiGraphics.fill(x - 2, height, x + width + 2, height + 1, -1);
        guiGraphics.fill(x - 2, y, x - 1, height, -1); // vertical
        guiGraphics.fill(x + width + 1, y, x + width + 2, height, -1);
    }

    public void addEntry(TradeRequest tradeRequest) {
        TradeRequestEntry entry = new TradeRequestEntry(tradeRequest);
        entry.setRemoveConsumer(tradeRequestEntry -> {
            modBase.getTradeRequestContainer().removeTradeRequest(tradeRequestEntry.tradeRequest);
            children.remove(tradeRequestEntry);
        });
        children.add(entry);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (TradeRequestEntry entry : children) {
            if (entry.isMouseOver(mouseX, mouseY)) {
                entry.mouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // No narration needed for this widget
    }

    public static class TradeRequestEntry {

        private static final ResourceLocation EMERALD_TEXTURE = new ResourceLocation("textures/item/emerald.png");
        private static final ResourceLocation ENCHANTED_BOOK_TEXTURE = new ResourceLocation("textures/item/enchanted_book.png");
        private final Font font = Minecraft.getInstance().font;
        public final TradeRequest tradeRequest;
        private int x, y1, x2, y2;

        private Consumer<TradeRequestEntry> removeConsumer;

        public TradeRequestEntry(TradeRequest request) {
            this.tradeRequest = request;
        }

        public void setRemoveConsumer(Consumer<TradeRequestEntry> removeConsumer) {
            this.removeConsumer = removeConsumer;
        }

        public void render(GuiGraphics guiGraphics, int index, int x, int y, int entryWidth) {
            this.x = x;
            this.y1 = y + (index * ENTRY_HEIGHT) + (5 * index);
            this.x2 = x + entryWidth;
            this.y2 = y + ENTRY_HEIGHT * (index + 1) + (5 * index);

            guiGraphics.fill(x, y1, x2, y2, 0xF0070707);

            guiGraphics.blit(ENCHANTED_BOOK_TEXTURE, x, y1, 0, 0, 16, 16, 16, 16);
            guiGraphics.blit(EMERALD_TEXTURE, x, y1 + 16, 0, 0, 16, 16, 16, 16);

            String enchantmentName = tradeRequest.enchantment().value().getFullname(tradeRequest.level()).getString();
            guiGraphics.drawString(font, enchantmentName, x + 20, y1 + 4, 0xFFFFFF, false);
            guiGraphics.drawString(font, Component.literal("§e" + tradeRequest.maxPrice()), x + 20, y1 + 20, 0xFFFFFF, false);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean clicked = isMouseOver(mouseX, mouseY);

            if (clicked && removeConsumer != null)
                removeConsumer.accept(this);
            return clicked;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return x <= mouseX && mouseX <= x2 && y1 <= mouseY && mouseY <= y2;
        }
    }
}
