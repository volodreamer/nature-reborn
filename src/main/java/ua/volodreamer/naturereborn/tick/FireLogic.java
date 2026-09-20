package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

public final class FireLogic {
	private FireLogic() {
	}

	public static boolean isFire(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.FIRE || block == Blocks.SOUL_FIRE || block instanceof FireBlock;
	}

	public static void tick(ServerLevel level, BlockPos pos, BlockState state, NatureRebornConfig config) {
		if (!config.fireEnabled || !isFire(state)) {
			return;
		}
		if (state.hasProperty(BlockStateProperties.AGE_15)) {
			int age = state.getValue(BlockStateProperties.AGE_15);
			int next = config.fireBurnUntilConsumed ? 0 : Math.max(0, age - 5);
			if (next != age) {
				level.setBlock(pos, state.setValue(BlockStateProperties.AGE_15, next), Block.UPDATE_CLIENTS);
			}
		}
		if (level.getRandom().nextFloat() < 0.28f) {
			nudgeSpread(level, pos);
		}
	}

	private static void nudgeSpread(ServerLevel level, BlockPos pos) {
		Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
		BlockPos target = pos.relative(dir);
		if (!level.isEmptyBlock(target)) {
			target = pos.relative(dir).above();
		}
		if (!level.isEmptyBlock(target) && !ForestEcology.isFoliage(level.getBlockState(target).getBlock())) {
			return;
		}
		BlockState fire = Blocks.FIRE.defaultBlockState();
		if (fire.canSurvive(level, target)) {
			level.setBlock(target, fire, Block.UPDATE_ALL);
		}
	}
}
