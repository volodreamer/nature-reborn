package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

public final class Weathering {
	private Weathering() {
	}

	public static boolean isWeatherable(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.COBBLESTONE
				|| block == Blocks.MOSSY_COBBLESTONE
				|| block == Blocks.STONE_BRICKS
				|| block == Blocks.MOSSY_STONE_BRICKS;
	}

	public static void tick(ServerLevel level, BlockPos pos, BlockState state, NatureRebornConfig config) {
		if (!config.weatheringEnabled) {
			return;
		}
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		double speed = config.clampedSpeed();
		if (speed <= 0) {
			return;
		}
		boolean wet = isDamp(level, pos);
		Block block = state.getBlock();
		if (block == Blocks.COBBLESTONE && wet && level.getRandom().nextDouble() < 0.035 * speed) {
			level.setBlock(pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
			return;
		}
		if (block == Blocks.STONE_BRICKS && wet && level.getRandom().nextDouble() < 0.02 * speed) {
			level.setBlock(pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
			return;
		}
		if (!wet && !level.isRainingAt(pos.above()) && level.getRandom().nextDouble() < 0.006 * speed) {
			if (block == Blocks.MOSSY_COBBLESTONE) {
				level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
			} else if (block == Blocks.MOSSY_STONE_BRICKS) {
				level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private static boolean isDamp(ServerLevel level, BlockPos pos) {
		if (level.isRainingAt(pos.above())) {
			return true;
		}
		for (Direction direction : Direction.values()) {
			Block neighbor = level.getBlockState(pos.relative(direction)).getBlock();
			if (neighbor == Blocks.WATER || neighbor == Blocks.MOSS_BLOCK || neighbor == Blocks.MOSS_CARPET) {
				return true;
			}
		}
		return false;
	}
}
