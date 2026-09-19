package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;

/**
 * Phase 2 world mutations: grass cover and sapling auto-plant.
 */
public final class NatureActions {
	private static final double BASE_GRASS_SPREAD = 0.06;
	private static final double BASE_GRASS_REGROW = 0.012;
	private static final double BASE_GRASS_DEATH = 0.02;
	private static final double BASE_SAPLING_PLANT = 0.025;
	private static final int SPREAD_RADIUS = 3;
	private static final int MIN_LIGHT = 9;

	private NatureActions() {
	}

	public static void apply(ServerLevel level, BlockPos pos, BlockState state, Species species, BiomeRates rates, NatureRebornConfig config, TickStats stats) {
		RandomSource random = level.getRandom();
		double speed = config.clampedSpeed();
		if (speed <= 0) {
			return;
		}
		switch (species.kind()) {
			case GRASS -> {
				if (config.grassEnabled) {
					handleGrass(level, pos, state, rates, random, speed, stats);
				}
			}
			case TREE -> {
				if (config.treesEnabled) {
					handleSaplingPlant(level, pos, species, rates, random, speed, stats);
				}
			}
			default -> {
			}
		}
	}

	private static void handleGrass(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block block = state.getBlock();

		if (chance(random, BASE_GRASS_DEATH * rates.death() * speed)) {
			if (block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN) {
				level.destroyBlock(pos, false);
				stats.grassDeaths++;
				return;
			}
			if (block == Blocks.GRASS_BLOCK && level.getRawBrightness(pos.above(), 0) < MIN_LIGHT) {
				level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
				stats.grassDeaths++;
				return;
			}
		}

		if (block == Blocks.GRASS_BLOCK && chance(random, BASE_GRASS_SPREAD * rates.spread() * speed)) {
			BlockPos target = offset(pos, random, SPREAD_RADIUS);
			if (canBecomeGrass(level, target)) {
				level.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
				stats.grassSpreads++;
			}
		}

		if (block == Blocks.GRASS_BLOCK && chance(random, BASE_GRASS_REGROW * rates.growth() * speed)) {
			BlockPos above = pos.above();
			if (level.isEmptyBlock(above)
					&& level.getRawBrightness(above, 0) >= MIN_LIGHT
					&& nearbyFoliage(level, above) < 3) {
				level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_ALL);
				stats.grassRegrows++;
			}
		}
	}

	private static void handleSaplingPlant(ServerLevel level, BlockPos origin, Species species, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block plant = species.plant();
		if (plant == null || plant == Blocks.AIR) {
			return;
		}
		if (!chance(random, BASE_SAPLING_PLANT * rates.spread() * speed)) {
			return;
		}
		BlockPos ground = offset(origin, random, SPREAD_RADIUS);
		BlockPos air = ground.above();
		if (!isPlantableSoil(level.getBlockState(ground).getBlock())) {
			return;
		}
		if (!level.isEmptyBlock(air)) {
			return;
		}
		if (level.getRawBrightness(air, 0) < MIN_LIGHT) {
			return;
		}
		BlockState sapling = plant.defaultBlockState();
		if (sapling.getBlock() instanceof DoublePlantBlock) {
			return;
		}
		level.setBlock(air, sapling, Block.UPDATE_ALL);
		stats.saplingsPlanted++;
	}

	private static int nearbyFoliage(ServerLevel level, BlockPos center) {
		int count = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				Block block = level.getBlockState(center.offset(dx, 0, dz)).getBlock();
				if (isReplaceableFoliage(block)) {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean canBecomeGrass(ServerLevel level, BlockPos pos) {
		Block ground = level.getBlockState(pos).getBlock();
		if (ground != Blocks.DIRT && ground != Blocks.ROOTED_DIRT) {
			return false;
		}
		BlockPos above = pos.above();
		return (level.isEmptyBlock(above) || isReplaceableFoliage(level.getBlockState(above).getBlock()))
				&& level.getRawBrightness(above, 0) >= MIN_LIGHT;
	}

	private static boolean isPlantableSoil(Block block) {
		return block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.PODZOL
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.ROOTED_DIRT;
	}

	private static boolean isReplaceableFoliage(Block block) {
		return block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.TALL_GRASS;
	}

	private static BlockPos offset(BlockPos origin, RandomSource random, int radius) {
		int dx = random.nextInt(radius * 2 + 1) - radius;
		int dz = random.nextInt(radius * 2 + 1) - radius;
		int dy = random.nextInt(3) - 1;
		return origin.offset(dx, dy, dz);
	}

	private static boolean chance(RandomSource random, double probability) {
		if (probability <= 0) {
			return false;
		}
		if (probability >= 1) {
			return true;
		}
		return random.nextDouble() < probability;
	}
}
