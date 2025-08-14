package de.gamedude.easyvillagertrade.core;

import de.gamedude.easyvillagertrade.utils.TradingState;
import java.util.Optional;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.AABB;

public class SelectionInterface {

    private final EasyVillagerTradeBase modBase;
    private Villager villager;
    private BlockPos lecternPos;

    public SelectionInterface(EasyVillagerTradeBase modBase) {
        this.modBase = modBase;
    }

    public Villager getVillager() {
        return villager;
    }

    public void setVillager(Villager villager) {
        this.villager = villager;
    }

    public BlockPos getLecternPos() {
        return lecternPos;
    }

    public void setLecternPos(BlockPos blockPos) {
        this.lecternPos = blockPos;
    }

    public int selectClosestToPlayer(LocalPlayer player) {
        Optional<BlockPos> closestBlockOptional = BlockPos.findClosestMatch(
            player.blockPosition(),
            3,
            3,
            blockPos ->
                player.level().getBlockState(blockPos).getBlock() instanceof LecternBlock
        );

        if (!closestBlockOptional.isPresent()) {
            modBase.setState(TradingState.INACTIVE);
            return 1;
        }
        this.lecternPos = closestBlockOptional.get();

        this.villager = getClosestEntity(player.level(), this.lecternPos);
        if (this.villager == null) {
            modBase.setState(TradingState.INACTIVE);
            return 2;
        }
        modBase.setState(TradingState.INACTIVE);
        return 0;
    }

    private Villager getClosestEntity(Level world, BlockPos blockPos) {
        Villager entity = null;
        double dist = Double.MAX_VALUE;

        for (Villager villagerEntity : world.getEntitiesOfClass(
            Villager.class,
            new AABB(blockPos).inflate(3),
            villager ->
                villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN
        )) {
            double distanceSquared = villagerEntity.distanceToSqr(
                blockPos.getX(),
                blockPos.getY(),
                blockPos.getZ()
            );
            if (distanceSquared < dist) {
                dist = distanceSquared;
                entity = villagerEntity;
            }
        }
        return entity;
    }

    public boolean isVillagerSleeping() {
        if (villager == null) {
            modBase.setVillagerSleeping(false);
            return false;
        }
        boolean sleeping = villager.isSleeping();
        modBase.setVillagerSleeping(sleeping);
        return sleeping;
    }
}
