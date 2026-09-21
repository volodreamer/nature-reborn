package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

final class LeafLitter {
	private static final double BASE_PLACE = 0.01;

	private LeafLitter() {
	}

	static void tick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, double speed, TickStats stats) {
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		double cap = coverageCap(level, pos);
		if (cap <= 0) {
			return;
		}
		BlockPos soil = state.getBlock() == Blocks.GRASS_BLOCK || ForestEcology.isPlantableSoil(state.getBlock())
				? pos
				: pos.below();
		BlockPos surface = soil.above();
		if (!ForestEcology.isUnderCanopy(level, surface)) {
			return;
		}
		double covered = coverage(level, soil);
		if (covered >= cap) {
			return;
		}
		if (random.nextDouble() >= BASE_PLACE * speed * (1.0 - covered / cap)) {
			return;
		}

		BlockState existing = level.getBlockState(surface);
		if (existing.getBlock() == Blocks.LEAF_LITTER) {
			thicken(level, surface, existing);
			stats.grassRegrows++;
			return;
		}
		if (!existing.isAir() && !ForestEcology.isFoliage(existing.getBlock())) {
			return;
		}
		BlockState litter = Blocks.LEAF_LITTER.defaultBlockState();
		if (litter.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			litter = litter.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		}
		litter = withSegments(litter, 1);
		level.setBlock(surface, litter, Block.UPDATE_ALL);
		stats.grassRegrows++;
	}

	private static void thicken(ServerLevel level, BlockPos pos, BlockState state) {
		IntegerProperty amount = segmentProperty(state);
		if (amount == null) {
			return;
		}
		int current = state.getValue(amount);
		if (current < 4) {
			level.setBlock(pos, state.setValue(amount, current + 1), Block.UPDATE_ALL);
		}
	}

	private static BlockState withSegments(BlockState state, int value) {
		IntegerProperty amount = segmentProperty(state);
		if (amount == null) {
			return state;
		}
		return state.setValue(amount, Math.min(value, 4));
	}

	private static IntegerProperty segmentProperty(BlockState state) {
		if (state.hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
			return BlockStateProperties.FLOWER_AMOUNT;
		}
		try {
			if (state.hasProperty(BlockStateProperties.SEGMENT_AMOUNT)) {
				return BlockStateProperties.SEGMENT_AMOUNT;
			}
		} catch (NoSuchFieldError ignored) {
			return null;
		}
		return null;
	}

	private static double coverage(ServerLevel level, BlockPos soil) {
		int litter = 0;
		int samples = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				samples++;
				BlockPos ground = soil.offset(dx, 0, dz);
				Block above = level.getBlockState(ground.above()).getBlock();
				if (above == Blocks.LEAF_LITTER) {
					litter++;
				}
			}
		}
		return samples == 0 ? 0 : (double) litter / samples;
	}

	static double coverageCap(ServerLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		String id = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		if (id.contains("taiga") || id.contains("grove") || id.contains("spruce")) {
			return 0;
		}
		if (id.contains("dark_forest")) {
			return 0.80;
		}
		if (id.contains("flower_forest")) {
			return 0.55;
		}
		if (id.contains("cherry")) {
			return 0.75;
		}
		if (id.contains("wooded_badlands")) {
			return 0.60;
		}
		if (id.contains("jungle")) {
			return 0.35;
		}
		if (id.contains("birch") || id.contains("forest")) {
			return 0.70;
		}
		if (biome.is(BiomeTags.IS_FOREST) && !biome.is(BiomeTags.IS_TAIGA)) {
			return 0.65;
		}
		return 0;
	}
}
