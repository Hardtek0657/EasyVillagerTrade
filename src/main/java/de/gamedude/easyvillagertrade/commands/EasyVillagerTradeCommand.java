package de.gamedude.easyvillagertrade.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.gamedude.easyvillagertrade.core.EasyVillagerTradeBase;
import de.gamedude.easyvillagertrade.utils.TradeRequest;
import de.gamedude.easyvillagertrade.utils.TradingState;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = "easyvillagertrade", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EasyVillagerTradeCommand {

    private static EasyVillagerTradeBase modBase;

    public static void setModBase(EasyVillagerTradeBase base) {
        modBase = base;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        String command_base = "evt";
        dispatcher.register(Commands.literal(command_base)
                .then(Commands.literal("select")
                        .then(Commands.literal("close").executes(EasyVillagerTradeCommand::executeSelectionClosest))
                        .executes(EasyVillagerTradeCommand::executeSelection))
                .then(Commands.literal("search")
                        .then(Commands.literal("add")
                                .then(Commands.argument("maxPrice", IntegerArgumentType.integer(1, 64))
                                        .then(Commands.argument("enchantment", StringArgumentType.word())
                                                .executes(context -> executeAddTradeRequest(context, IntegerArgumentType.getInteger(context, "maxPrice"), 1))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                                        .executes(context -> executeAddTradeRequest(context, IntegerArgumentType.getInteger(context, "maxPrice"), IntegerArgumentType.getInteger(context, "level")))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("enchantment", StringArgumentType.word())
                                        .executes(EasyVillagerTradeCommand::executeRemoveTradeRequest)))
                        .then(Commands.literal("list").executes(EasyVillagerTradeCommand::executeListTradeRequest)))
                .then(Commands.literal("execute").executes(EasyVillagerTradeCommand::executeVillagerTrade))
                .then(Commands.literal("stop").executes(ctx -> {
                    if (modBase != null) {
                        modBase.setState(TradingState.INACTIVE);
                    }
                    return 1;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.translatable("evt.command.basic_usage"), false);
                    return 1;
                }));
    }

    public static int executeAddTradeRequest(CommandContext<CommandSourceStack> context, int maxPrice, int level) {
        if (modBase == null) return 0;

        try {
            String enchantmentName = StringArgumentType.getString(context, "enchantment");
            Holder<Enchantment> enchantmentHolder = modBase.getTradeRequestInputHandler().getEnchantment(enchantmentName);

            if (enchantmentHolder == null) {
                context.getSource().sendFailure(Component.literal("Invalid enchantment: " + enchantmentName));
                return 0;
            }

            Enchantment enchantment = enchantmentHolder.value();
            TradeRequest tradeRequest = modBase.getTradeRequestInputHandler().parseCommandInput(enchantment, level, maxPrice);
            modBase.getTradeRequestContainer().addTradeRequest(tradeRequest);

            context.getSource().sendSuccess(() -> Component.translatable("evt.command.add",
                "§e" + enchantment.getFullname(level).getString(),
                "§a" + tradeRequest.maxPrice()), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int executeRemoveTradeRequest(CommandContext<CommandSourceStack> context) {
        if (modBase == null) return 0;

        try {
            String enchantmentName = StringArgumentType.getString(context, "enchantment");
            Holder<Enchantment> enchantmentHolder = modBase.getTradeRequestInputHandler().getEnchantment(enchantmentName);

            if (enchantmentHolder == null) {
                context.getSource().sendFailure(Component.literal("Invalid enchantment: " + enchantmentName));
                return 0;
            }

            Enchantment enchantment = enchantmentHolder.value();
            modBase.getTradeRequestContainer().removeTradeRequestByEnchantment(enchantment);

            boolean multipleLevels = enchantment.getMaxLevel() == 1;
            String[] parts = enchantment.getFullname(1).getString().split(" ");
            String name = Strings.join((multipleLevels) ? parts : Arrays.copyOf(parts, parts.length - 1), " ");

            context.getSource().sendSuccess(() -> Component.translatable("evt.command.remove", "§e" + StringUtils.capitalize(name)), false);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int executeListTradeRequest(CommandContext<CommandSourceStack> context) {
        if (modBase == null) return 0;

        context.getSource().sendSuccess(() -> Component.translatable("evt.command.list.head"), false);
        modBase.getTradeRequestContainer().getTradeRequests().forEach(offer ->
                context.getSource().sendSuccess(() -> Component.translatable("evt.command.list.body",
                "§e" + offer.enchantment().value().getFullname(offer.level()).getString(),
                "§a" + offer.maxPrice()), false));
        return 1;
    }

    public static int executeSelection(CommandContext<CommandSourceStack> context) {
        if (modBase == null) return 0;

        modBase.setState(TradingState.MODE_SELECTION);
        context.getSource().sendSuccess(() -> Component.translatable("evt.command.selecting"), false);
        return 1;
    }

    public static int executeSelectionClosest(CommandContext<CommandSourceStack> context) {
        if (modBase == null) return 0;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        int x = modBase.getSelectionInterface().selectClosestToPlayer(player);
        switch (x) {
            case 1 -> player.sendSystemMessage(Component.translatable("evt.logic.select.fail_lectern"));
            case 2 -> player.sendSystemMessage(Component.translatable("evt.logic.select.fail_villager"));
            case 0 -> player.sendSystemMessage(Component.translatable("evt.logic.select.success"));
        }
        return 1;
    }

    public static int executeVillagerTrade(CommandContext<CommandSourceStack> context) {
        if (modBase == null) return 0;

        modBase.setState(TradingState.CHECK_OFFERS);

        if(modBase.getSelectionInterface().getVillager() == null || modBase.getSelectionInterface().getLecternPos() == null) {
            context.getSource().sendSuccess(() -> Component.translatable("evt.command.not_selected"), false);
            return 1;
        }

        context.getSource().sendSuccess(() -> Component.translatable("evt.command.execute"), false);
        modBase.handleInteractionWithVillager();
        return 1;
    }
}
