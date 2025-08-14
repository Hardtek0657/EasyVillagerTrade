package de.gamedude.easyvillagertrade.screen.widget;

import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

public class EnchantmentInputWidget extends EditBox {

    private String suggestion;

    public EnchantmentInputWidget(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.empty());
        setSuggestion("Enchantment");
        this.setResponder(getChangeListener());
    }

    private Consumer<String> getChangeListener() {
        return text -> {
            if (BuiltInRegistries.ENCHANTMENT.stream()
                .anyMatch(enchantment ->
                    enchantment.getFullname(1).getString().equalsIgnoreCase(text.trim()) ||
                    enchantment.getDescriptionId().toLowerCase().contains(text.trim().toLowerCase()))) {
                this.setTextColor(0xFFFF00);
            } else {
                this.setTextColor(0xE0E0E0);
            }

            suggestion = getPossibleEnchantmentNameOrElse(text)
                .toLowerCase()
                .replaceFirst(text.toLowerCase().replace("+", ""), "");
            setSuggestion(suggestion);
        };
    }

    private String getPossibleEnchantmentNameOrElse(String input) {
        String enchantmentName = null;
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            boolean multipleLevels = enchantment.getMaxLevel() == 1;
            String[] parts = enchantment.getFullname(1).getString().split(" ");
            String name = String.join(" ",
                (multipleLevels) ? parts : Arrays.copyOf(parts, parts.length - 1));

            if (name.toLowerCase().startsWith(input.toLowerCase())) {
                enchantmentName = name;
                break;
            }
        }
        return enchantmentName != null ? enchantmentName.substring(input.length()) : "";
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setEnchantmentText();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            setEnchantmentText();
            return false; // Allow tab navigation
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setEnchantmentText() {
        setValue(StringUtils.capitalize(getValue() + ((suggestion == null) ? "" : suggestion)));
    }
}
