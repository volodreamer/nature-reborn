package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

public final class AutoReplant {
	private AutoReplant() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(AutoReplant::afterBreak);
	}

	private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		NatureRebornConfig config = NatureReborn.config();
		if (!config.masterEnabled || !config.cropsEnabled || !config.autoReplantEnabled) {
			return;
		}
		Block crop = state.getBlock();
		if (!CropLogic.isCropBlock(crop) || !CropLogic.isMature(state)) {
			return;
		}
		if (!CropLogic.canPlantAt(server, pos, crop, config)) {
			return;
		}
		Item seed = CropLogic.seedItem(crop);
		boolean paid = CropLogic.consumeSeed(player, seed);
		if (!paid && !config.autoReplantAllSources) {
			return;
		}
		server.setBlock(pos, CropLogic.seedState(crop), Block.UPDATE_ALL);
	}
}
