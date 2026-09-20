package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NatureActions {
	private static final double BASE_GRASS_SPREAD = 0.06;
	private static final double BASE_COVER_GROW = 0.018;
	private static final double BASE_GRASS_GROW_UP = 0.04;
	private static final double BASE_PLANT_DEATH = 0.018;
	private static final double BASE_FLOWER_SPREAD = 0.035;
	private static final double BASE_SAPLING_PLANT = 0.045;
	private static final double BASE_SAPLING_GROW = 0.14;
	private static final double BASE_SAPLING_DEATH = 0.05;
	private static final double BASE_TREE_DEATH = 0.0035;
	private static final int SPREAD_RADIUS = 5;
	private static final int MIN_LIGHT = 8;
	private static final int MAX_TREE_LOGS = 220;
	private static final int MAX_TREE_LEAVES = 400;
	private static final int MIN_TREE_TRUNK = 4;
	private static final int MIN_TREE_LEAVES = 8;

	private static final Block[] GROUND_COVER = {
			Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
			Blocks.FERN
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
					handleTree(level, pos, state, species, rates, random, speed, config, stats);
				}
			}
			case CROP, NETHER -> CropLogic.tick(level, pos, state, species, rates, config, stats);
			default -> {
			}
		}
	}

	private static void handleGrass(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block block = state.getBlock();
		boolean shaded = block == Blocks.GRASS_BLOCK
				? ForestEcology.isUnderCanopy(level, pos.above())
				: ForestEcology.isUnderCanopy(level, pos);

		double shadeDeath = shaded ? 4.5 : 1.0;
		if (isReplaceableFoliage(block) && chance(random, BASE_PLANT_DEATH * rates.death() * speed * shadeDeath)) {
			level.destroyBlock(pos, false);
			stats.grassDeaths++;
			return;
		}

		if (shaded || VillageZones.inVillage(level, pos)) {
			return;
		}

		if (block == Blocks.GRASS_BLOCK && chance(random, BASE_GRASS_SPREAD * rates.spread() * speed)) {
			BlockPos target = offset(pos, random, SPREAD_RADIUS);
			if (canBecomeGrass(level, target) && !ForestEcology.isUnderCanopy(level, target.above()) && !VillageZones.inVillage(level, target)) {
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
			stats.grassRegrows++;
		}
	}

	private static void handleFlower(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block block = state.getBlock();
		boolean shaded = ForestEcology.isUnderCanopy(level, pos);
		double shadeDeath = shaded ? 5.0 : 1.0;
		if (chance(random, BASE_PLANT_DEATH * rates.death() * speed * shadeDeath)) {
			level.destroyBlock(pos, false);
			stats.flowerDeaths++;
			return;
		}
		if (shaded || VillageZones.inVillage(level, pos)) {
			return;
		}
		if (!chance(random, BASE_FLOWER_SPREAD * rates.spread() * speed)) {
			return;
		}
		BlockPos ground = offset(pos.below(), random, SPREAD_RADIUS);
		if (isProtectedSurface(level.getBlockState(ground)) || VillageZones.inVillage(level, ground)) {
			return;
		}
		if (!isPlantableSoil(level.getBlockState(ground).getBlock())) {
			return;
		}
		BlockPos air = ground.above();
		if (!level.isEmptyBlock(air) || level.getRawBrightness(air, 0) < MIN_LIGHT) {
			return;
		}
		if (ForestEcology.isUnderCanopy(level, air)) {
			return;
		}
		level.setBlock(air, block.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static void handleTree(ServerLevel level, BlockPos pos, BlockState state, Species species, BiomeRates rates, RandomSource random, double speed, NatureRebornConfig config, TickStats stats) {
		Block block = state.getBlock();
		if (block instanceof SaplingBlock sapling) {
			handleSapling(level, pos, state, sapling, species, rates, random, speed, stats);
			return;
		}
		if (state.is(BlockTags.LOGS) && ForestEcology.isTrunkBase(level, pos) && ForestEcology.clearLeaflessTrunk(level, pos)) {
			stats.treeDeaths++;
			return;
		}
		if (state.is(BlockTags.LOGS) && ForestEcology.isTrunkBase(level, pos) && isNaturalTreeStart(level, pos)) {
			double deathChance = BASE_TREE_DEATH * rates.death() * speed * ForestEcology.deathMultiplier(level, pos);
			if (chance(random, deathChance)) {
				killTree(level, pos, species, random, config, stats);
				return;
			}
		}
		if (state.is(BlockTags.LEAVES) || isNaturalTreeStart(level, pos)) {
			handleSaplingPlant(level, pos, species, rates, random, speed, stats);
		}
	}

	private static boolean isNaturalTreeStart(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
			return false;
		}
		return ForestEcology.trunkColumnHeight(level, pos) >= MIN_TREE_TRUNK || hasNearbyLeaves(level, pos);
	}

	private static boolean hasNearbyLeaves(ServerLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(pos.relative(direction)).is(BlockTags.LEAVES)) {
				return true;
			}
		}
		for (int dy = 1; dy <= 6; dy++) {
			if (level.getBlockState(pos.above(dy)).is(BlockTags.LEAVES)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDecorativeWood(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
			return false;
		}
		return ForestEcology.trunkColumnHeight(level, pos) < 3 && !hasNearbyLeaves(level, pos);
	}

	private static void handleSapling(ServerLevel level, BlockPos pos, BlockState state, SaplingBlock sapling, Species species, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		if (VillageZones.inVillage(level, pos)) {
			if (chance(random, BASE_SAPLING_DEATH * rates.death() * speed * 4.0)) {
				level.destroyBlock(pos, false);
				stats.saplingDeaths++;
			}
			return;
		}
		boolean twoByTwo = ForestEcology.isTwoByTwoSapling(level, pos);
		if (!twoByTwo && ForestEcology.needsTwoByTwo(species)) {
			if (!ForestEcology.tooCloseForMega(level, pos) && ForestEcology.tryPlaceTwoByTwo(level, pos, sapling)) {
				twoByTwo = true;
				stats.saplingsPlanted += 3;
			} else if (chance(random, BASE_SAPLING_DEATH * rates.death() * speed * 2.5)) {
				level.destroyBlock(pos, false);
				stats.saplingDeaths++;
				return;
			}
		}

		if (twoByTwo) {
			if (chance(random, BASE_SAPLING_GROW * rates.growth() * speed * 1.6)) {
				growSapling(level, pos, state, sapling, stats);
			}
			return;
		}

		boolean crowded = nearbySaplings(level, pos) > 3 || ForestEcology.isInterior(level, pos);
		boolean dark = level.getRawBrightness(pos, 0) < MIN_LIGHT;
		boolean blocked = !hasGrowSpace(level, pos);
		boolean canGrow = !crowded && !dark && !blocked;

		if (!canGrow && chance(random, BASE_SAPLING_DEATH * rates.death() * speed * 3.0)) {
			level.destroyBlock(pos, false);
			stats.saplingDeaths++;
			return;
		}

		if (canGrow && chance(random, BASE_SAPLING_GROW * rates.growth() * speed)) {
			growSapling(level, pos, state, sapling, stats);
		}
	}

	private static void growSapling(ServerLevel level, BlockPos pos, BlockState state, SaplingBlock sapling, TickStats stats) {
		ItemStack meal = new ItemStack(Items.BONE_MEAL);
		boolean grew = BoneMealItem.growCrop(meal, level, pos);
		if (!grew) {
			level.scheduleTick(pos, sapling, 1);
		}
		if (!(level.getBlockState(pos).getBlock() instanceof SaplingBlock)) {
			stats.saplingGrowths++;
		}
	}

	private static boolean hasGrowSpace(ServerLevel level, BlockPos pos) {
		for (int dy = 1; dy <= 6; dy++) {
			BlockState above = level.getBlockState(pos.above(dy));
			if (!above.isAir() && !above.is(BlockTags.LEAVES) && !isReplaceableFoliage(above.getBlock())) {
				return false;
			}
		}
		return true;
	}

	private static void killTree(ServerLevel level, BlockPos origin, Species species, RandomSource random, NatureRebornConfig config, TickStats stats) {
		List<BlockPos> logs = new ArrayList<>();
		List<BlockPos> leaves = new ArrayList<>();
		floodTree(level, origin, species, logs, leaves);
		int tallest = 0;
		for (BlockPos log : logs) {
			tallest = Math.max(tallest, ForestEcology.trunkColumnHeight(level, log));
		}
		if (logs.size() < 4 || (leaves.size() < MIN_TREE_LEAVES && tallest < MIN_TREE_TRUNK)) {
			return;
		}

		BlockPos lowest = logs.getFirst();
		for (BlockPos log : logs) {
			if (log.getY() < lowest.getY()) {
				lowest = log;
			}
		}
		BlockState sample = level.getBlockState(lowest);
		List<BlockPos> hives = ForestEcology.collectAdjacentHives(level, logs);
		hives.addAll(ForestEcology.collectAdjacentHives(level, leaves));

		for (BlockPos leaf : leaves) {
			level.destroyBlock(leaf, false);
		}
		for (BlockPos log : logs) {
			level.destroyBlock(log, false);
		}
		ForestEcology.clearOrphanLogs(level, lowest, species);
		ForestEcology.relocateHives(level, hives, random);

		if (config.fallenLogsEnabled) {
			ForestEcology.placeFallenLog(level, lowest, logs, sample, random);
		}

		if (species.plant() != null && species.plant() != Blocks.AIR && random.nextFloat() < 0.22f) {
			BlockPos edge = ForestEcology.edgePlantSpot(level, lowest, random);
			if (edge != null && !VillageZones.inVillage(level, edge)) {
				if (ForestEcology.needsTwoByTwo(species)) {
					if (ForestEcology.tryPlaceTwoByTwo(level, edge, species.plant())) {
						stats.saplingsPlanted += 4;
					}
				} else {
					level.setBlock(edge, species.plant().defaultBlockState(), Block.UPDATE_ALL);
					stats.saplingsPlanted++;
				}
			}
		}
		stats.treeDeaths++;
	}

	private static void floodTree(ServerLevel level, BlockPos origin, Species species, List<BlockPos> logs, List<BlockPos> leaves) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
			BlockPos current = queue.removeFirst();
			BlockState state = level.getBlockState(current);
			if (!species.matches(state.getBlock()) && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
				continue;
			}
			if (state.is(BlockTags.LOGS) && species.matches(state.getBlock())) {
				if (isDecorativeWood(level, current)) {
					continue;
				}
				logs.add(current);
			} else if (state.is(BlockTags.LEAVES) && species.matches(state.getBlock())) {
				if (leaves.size() < MAX_TREE_LEAVES) {
					leaves.add(current);
				}
			} else if (!species.matches(state.getBlock())) {
				continue;
			}
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (seen.add(next) && current.distManhattan(origin) < 28) {
					queue.add(next);
				}
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
		BlockPos probe = offset(origin, random, SPREAD_RADIUS);
		BlockPos ground = ForestEcology.walkToSoil(level, probe);
		if (ground == null || ForestEcology.isProtected(level.getBlockState(ground))) {
			return;
		}
		if (VillageZones.inVillage(level, ground)) {
			return;
		}
		BlockPos air = ground.above();
		if (!level.isEmptyBlock(air) && !isReplaceableFoliage(level.getBlockState(air).getBlock()) && !(level.getBlockState(air).getBlock() instanceof SaplingBlock)) {
			return;
		}
		if (level.getRawBrightness(air, 0) < MIN_LIGHT) {
			return;
		}
		if (ForestEcology.isInterior(level, air)) {
			return;
		}
		if (!hasGrowSpace(level, air)) {
			return;
		}
		if (plant.defaultBlockState().getBlock() instanceof DoublePlantBlock) {
			return;
		}
		if (ForestEcology.needsTwoByTwo(species)) {
			if (ForestEcology.tryPlaceTwoByTwo(level, air, plant)) {
				stats.saplingsPlanted += 4;
			}
			return;
		}
		if (nearbySaplings(level, air) > 0) {
			return;
		}
		level.setBlock(air, plant.defaultBlockState(), Block.UPDATE_ALL);
		stats.saplingsPlanted++;
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

	private static int nearbyFoliage(ServerLevel level, BlockPos center) {
		int count = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (isReplaceableFoliage(level.getBlockState(center.offset(dx, 0, dz)).getBlock())) {
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
				if (dx == 0 && dz == 0) {
					continue;
				}
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
		if (ground != Blocks.DIRT && ground != Blocks.ROOTED_DIRT && ground != Blocks.COARSE_DIRT) {
			return false;
		}
		BlockPos above = pos.above();
		return (level.isEmptyBlock(above) || isReplaceableFoliage(level.getBlockState(above).getBlock()))
				&& level.getRawBrightness(above, 0) >= MIN_LIGHT;
	}

	private static boolean isProtectedSurface(BlockState state) {
		return ForestEcology.isProtected(state);
	}

	private static boolean isPlantableSoil(Block block) {
		return ForestEcology.isPlantableSoil(block);
	}

	private static boolean isReplaceableFoliage(Block block) {
		return ForestEcology.isFoliage(block);
	}

	private static BlockPos offset(BlockPos origin, RandomSource random, int radius) {
		return origin.offset(
				random.nextInt(radius * 2 + 1) - radius,
				random.nextInt(3) - 1,
				random.nextInt(radius * 2 + 1) - radius
		);
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
