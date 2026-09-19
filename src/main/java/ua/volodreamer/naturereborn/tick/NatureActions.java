package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;

public final class NatureActions {
	private static final double BASE_GRASS_SPREAD = 0.06;
	private static final double BASE_COVER_GROW = 0.018;
	private static final double BASE_GRASS_GROW_UP = 0.04;
	private static final double BASE_PLANT_DEATH = 0.018;
	private static final double BASE_FLOWER_SPREAD = 0.035;
	private static final double BASE_SAPLING_PLANT = 0.09;
	private static final int SPREAD_RADIUS = 3;
	private static final int MIN_LIGHT = 9;

	private static final Block[] GROUND_COVER = {
			Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
			Blocks.FERN,
			Blocks.DANDELION, Blocks.POPPY, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET
	};

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
			case PLANT -> {
				if (config.plantsEnabled) {
					handleFlower(level, pos, state, rates, random, speed, stats);
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

		if (isLivingCover(block) && chance(random, BASE_PLANT_DEATH * rates.death() * speed)) {
			destroyCover(level, pos, state);
			stats.grassDeaths++;
			return;
		}

		if (block == Blocks.GRASS_BLOCK && isProtectedSurface(level.getBlockState(pos))) {
			return;
		}

		if (block == Blocks.GRASS_BLOCK && chance(random, BASE_GRASS_SPREAD * rates.spread() * speed)) {
			BlockPos target = offset(pos, random, SPREAD_RADIUS);
			if (canBecomeGrass(level, target)) {
				level.setBlock(target, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
				stats.grassSpreads++;
			}
		}

		if (block == Blocks.SHORT_GRASS && chance(random, BASE_GRASS_GROW_UP * rates.growth() * speed)) {
			if (level.isEmptyBlock(pos.above()) && nearbyFoliage(level, pos) < 6) {
				placeDouble(level, pos, Blocks.TALL_GRASS);
				stats.grassRegrows++;
			}
			return;
		}

		if (block == Blocks.FERN && chance(random, BASE_GRASS_GROW_UP * rates.growth() * speed * 0.6)) {
			if (level.isEmptyBlock(pos.above())) {
				placeDouble(level, pos, Blocks.LARGE_FERN);
				stats.grassRegrows++;
			}
			return;
		}

		if (block == Blocks.GRASS_BLOCK && chance(random, BASE_COVER_GROW * rates.growth() * speed)) {
			BlockPos above = pos.above();
			if (!level.isEmptyBlock(above) || level.getRawBrightness(above, 0) < MIN_LIGHT) {
				return;
			}
			if (nearbyFoliage(level, above) >= 4) {
				return;
			}
			Block cover = GROUND_COVER[random.nextInt(GROUND_COVER.length)];
			level.setBlock(above, cover.defaultBlockState(), Block.UPDATE_ALL);
			if (isFlower(cover)) {
				stats.flowerSpreads++;
			} else {
				stats.grassRegrows++;
			}
		}
	}

	private static void handleFlower(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block block = state.getBlock();
		if (chance(random, BASE_PLANT_DEATH * rates.death() * speed)) {
			level.destroyBlock(pos, false);
			stats.flowerDeaths++;
			return;
		}
		if (!chance(random, BASE_FLOWER_SPREAD * rates.spread() * speed)) {
			return;
		}
		BlockPos ground = offset(pos.below(), random, SPREAD_RADIUS);
		if (isProtectedSurface(level.getBlockState(ground))) {
			return;
		}
		if (!isPlantableSoil(level.getBlockState(ground).getBlock())) {
			return;
		}
		BlockPos air = ground.above();
		if (!level.isEmptyBlock(air) || level.getRawBrightness(air, 0) < MIN_LIGHT) {
			return;
		}
		level.setBlock(air, block.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static void handleSaplingPlant(ServerLevel level, BlockPos origin, Species species, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block plant = species.plant();
		if (plant == null || plant == Blocks.AIR) {
			return;
		}
		if (!chance(random, BASE_SAPLING_PLANT * rates.spread() * speed)) {
			return;
		}
		BlockPos probe = offset(origin, random, SPREAD_RADIUS);
		BlockPos ground = findSoil(level, probe);
		if (ground == null || isProtectedSurface(level.getBlockState(ground))) {
			return;
		}
		BlockPos air = ground.above();
		if (!level.isEmptyBlock(air) && !isReplaceableFoliage(level.getBlockState(air).getBlock())) {
			return;
		}
		if (level.getRawBrightness(air, 0) < MIN_LIGHT) {
			return;
		}
		if (nearbySaplings(level, air) > 0) {
			return;
		}
		BlockState sapling = plant.defaultBlockState();
		if (sapling.getBlock() instanceof DoublePlantBlock) {
			return;
		}
		level.setBlock(air, sapling, Block.UPDATE_ALL);
		stats.saplingsPlanted++;
	}

	private static BlockPos findSoil(ServerLevel level, BlockPos start) {
		BlockPos.MutableBlockPos cursor = start.mutable();
		for (int i = 0; i < 10; i++) {
			Block block = level.getBlockState(cursor).getBlock();
			if (isPlantableSoil(block)) {
				return cursor.immutable();
			}
			cursor.move(0, -1, 0);
		}
		return null;
	}

	private static void placeDouble(ServerLevel level, BlockPos lower, Block block) {
		BlockState base = block.defaultBlockState();
		if (!base.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			level.setBlock(lower, base, Block.UPDATE_ALL);
			return;
		}
		level.setBlock(lower, base.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
		level.setBlock(lower.above(), base.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	private static void destroyCover(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			level.destroyBlock(pos, false);
			return;
		}
		level.destroyBlock(pos, false);
	}

	private static int nearbyFoliage(ServerLevel level, BlockPos center) {
		int count = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				Block block = level.getBlockState(center.offset(dx, 0, dz)).getBlock();
				if (isReplaceableFoliage(block) || isFlower(block)) {
					count++;
				}
			}
		}
		return count;
	}

	private static int nearbySaplings(ServerLevel level, BlockPos center) {
		int count = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (level.getBlockState(center.offset(dx, 0, dz)).getBlock() instanceof SaplingBlock) {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean canBecomeGrass(ServerLevel level, BlockPos pos) {
		BlockState groundState = level.getBlockState(pos);
		if (isProtectedSurface(groundState)) {
			return false;
		}
		Block ground = groundState.getBlock();
		if (ground != Blocks.DIRT && ground != Blocks.ROOTED_DIRT) {
			return false;
		}
		BlockPos above = pos.above();
		return (level.isEmptyBlock(above) || isReplaceableFoliage(level.getBlockState(above).getBlock()))
				&& level.getRawBrightness(above, 0) >= MIN_LIGHT;
	}

	private static boolean isProtectedSurface(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.DIRT_PATH || block == Blocks.FARMLAND;
	}

	private static boolean isPlantableSoil(Block block) {
		return block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.PODZOL
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.ROOTED_DIRT;
	}

	private static boolean isReplaceableFoliage(Block block) {
		return block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.TALL_GRASS || block == Blocks.LARGE_FERN;
	}

	private static boolean isLivingCover(Block block) {
		return isReplaceableFoliage(block) || isFlower(block);
	}

	private static boolean isFlower(Block block) {
		return block == Blocks.DANDELION
				|| block == Blocks.POPPY
				|| block == Blocks.BLUE_ORCHID
				|| block == Blocks.ALLIUM
				|| block == Blocks.AZURE_BLUET
				|| block == Blocks.RED_TULIP
				|| block == Blocks.ORANGE_TULIP
				|| block == Blocks.WHITE_TULIP
				|| block == Blocks.PINK_TULIP
				|| block == Blocks.OXEYE_DAISY
				|| block == Blocks.CORNFLOWER
				|| block == Blocks.LILY_OF_THE_VALLEY;
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
