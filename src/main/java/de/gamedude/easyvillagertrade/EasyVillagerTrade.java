package de.gamedude.easyvillagertrade;

import de.gamedude.easyvillagertrade.commands.EasyVillagerTradeCommand;
import de.gamedude.easyvillagertrade.config.Config;
import de.gamedude.easyvillagertrade.core.EasyVillagerTradeBase;
import de.gamedude.easyvillagertrade.screen.TradeSelectScreen;
import de.gamedude.easyvillagertrade.utils.TradingState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.lwjgl.glfw.GLFW;

@Mod("easyvillagertrade")
@Mod.EventBusSubscriber(modid = "easyvillagertrade", bus = Mod.EventBusSubscriber.Bus.MOD)
public class EasyVillagerTrade {

    public static final Config CONFIG = new Config("easyvillagertrade");
    private static EasyVillagerTradeBase modBase;
    private static KeyMapping keyBinding;

    public EasyVillagerTrade() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyMappings);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        modBase = new EasyVillagerTradeBase();
        EasyVillagerTradeCommand.setModBase(modBase);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // Register commands and other client-side setup
        event.enqueueWork(() -> {
            // Client setup code here
        });
    }

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        keyBinding = new KeyMapping("key.custom.openscreen", GLFW.GLFW_KEY_F6, "EasyVillagerTrade");
        event.register(keyBinding);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() == InteractionHand.OFF_HAND || !event.getLevel().isClientSide())
            return;

        if (event.getTarget() instanceof Villager villager && modBase.getState() == TradingState.MODE_SELECTION) {
            modBase.getSelectionInterface().setVillager(villager);
            event.getEntity().sendSystemMessage(Component.translatable("evt.command.selected.villager"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.OFF_HAND || !event.getLevel().isClientSide())
            return;

        BlockPos blockPos = event.getPos();
        if (event.getLevel().getBlockState(blockPos).getBlock() == Blocks.LECTERN && modBase.getState() == TradingState.MODE_SELECTION) {
            modBase.getSelectionInterface().setLecternPos(blockPos);
            event.getEntity().sendSystemMessage(Component.translatable("evt.command.selected.lectern"));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            modBase.handle();
        } else if (event.phase == TickEvent.Phase.END) {
            Minecraft client = Minecraft.getInstance();
            while (keyBinding != null && keyBinding.consumeClick()) {
                client.setScreen(new TradeSelectScreen());
            }
        }
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenEvent.Opening event) {
        if (modBase.getState() == TradingState.CHECK_OFFERS && event.getScreen() instanceof MerchantScreen) {
            // Cancel the merchant screen opening and handle trades automatically
            event.setCanceled(true);
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                // Close the merchant screen immediately
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundContainerClosePacket(0));
                }
            });
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (modBase.getState() == TradingState.CHECK_OFFERS) {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                // Check if this is a merchant container and get the offers
                if (event.getContainer() instanceof net.minecraft.world.inventory.MerchantMenu merchantMenu) {
                    modBase.checkVillagerOffers(merchantMenu.getOffers());
                }
            });
        }
    }

    public static EasyVillagerTradeBase getModBase() {
        return modBase;
    }
}
