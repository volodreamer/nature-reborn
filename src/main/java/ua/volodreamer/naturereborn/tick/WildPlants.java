package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ua.volodreamer.naturereborn.species.BiomeRates;

final class WildPlants {
	private static final double BASE_GROW = 0.08;
	private static final double BASE_SPREAD = 0.04;
	private static final double BASE_DEATH = 0.012;

	private WildPlants() {
	}

	static boolean tick(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block block = state.getBlock();
		if (block instanceof CactusBlock || block == Blocks.CACTUS) {
			handleColumn(level, pos, Blocks.CACTUS, 3, WildPlants::cactusGround, rates, random, speed, stats, true);
			return true;
		}
		if (block instanceof SugarCaneBlock || block == Blocks.SUGAR_CANE) {
			handleColumn(level, pos, Blocks.SUGAR_CANE, 3, WildPlants::caneGround, rates, random, speed, stats, false);
			return true;
		}
		if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
			handleMushroom(level, pos, block, rates, random, speed, stats);
			return true;
		}
		if (block == Blocks.LILY_PAD) {
			handleLily(level, pos, rates, random, speed, stats);
			return true;
		}
		if (block == Blocks.DEAD_BUSH) {
			handleDeadBush(level, pos, rates, random, speed, stats);
			return true;
		}
		if (block == Blocks.SWEET_BERRY_BUSH) {
			handleSweetBerry(level, pos, state, rates, random, speed, stats);
			return true;
		}
		return false;
	}

	private interface GroundCheck {
		boolean test(ServerLevel level, BlockPos ground);
	}

	private static void handleSweetBerry(ServerLevel level, BlockPos pos, BlockState state, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block soil = level.getBlockState(pos.below()).getBlock();
		boolean valid = soil == Blocks.GRASS_BLOCK || soil == Blocks.DIRT || soil == Blocks.PODZOL
				|| soil == Blocks.COARSE_DIRT || soil == Blocks.ROOTED_DIRT || soil == Blocks.MOSS_BLOCK;
		if (!valid) {
			if (chance(random, BASE_DEATH * rates.death() * speed * 4.0)) {
				level.destroyBlock(pos, false);
				stats.flowerDeaths++;
			}
			return;
		}
		int age = state.hasProperty(BlockStateProperties.AGE_3) ? state.getValue(BlockStateProperties.AGE_3) : 3;
		if (age < 3 && chance(random, BASE_GROW * rates.growth() * speed)) {
			level.setBlock(pos, state.setValue(BlockStateProperties.AGE_3, age + 1), Block.UPDATE_ALL);
			stats.flowerSpreads++;
			return;
		}
		if (VillageZones.inVillage(level, pos) || age < 3) {
			return;
		}
		if (!chance(random, BASE_SPREAD * rates.spread() * speed)) {
			return;
		}
		int dist = 2 + random.nextInt(3);
		Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos probe = pos.relative(dir, dist).offset(0, random.nextInt(3) - 1, 0);
		BlockPos ground = ForestEcology.walkToSoil(level, probe);
		if (ground == null) {
			return;
		}
		BlockPos air = ground.above();
		if (VillageZones.inVillage(level, air) || ForestEcology.isProtected(level.getBlockState(ground))) {
			return;
		}
		Block groundBlock = level.getBlockState(ground).getBlock();
		if (groundBlock != Blocks.GRASS_BLOCK && groundBlock != Blocks.DIRT && groundBlock != Blocks.PODZOL && groundBlock != Blocks.COARSE_DIRT) {
			return;
		}
		if (!level.isEmptyBlock(air) && !ForestEcology.isFoliage(level.getBlockState(air).getBlock())) {
			return;
		}
		if (!Blocks.SWEET_BERRY_BUSH.defaultBlockState().canSurvive(level, air)) {
			return;
		}
		if (berryTooClose(level, air)) {
			return;
		}
		level.setBlock(air, Blocks.SWEET_BERRY_BUSH.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static boolean berryTooClose(ServerLevel level, BlockPos pos) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				if (Math.max(Math.abs(dx), Math.abs(dz)) < 2) {
					continue;
				}
				if (level.getBlockState(pos.offset(dx, 0, dz)).getBlock() == Blocks.SWEET_BERRY_BUSH) {
					return true;
				}
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (level.getBlockState(pos.offset(dx, 0, dz)).getBlock() == Blocks.SWEET_BERRY_BUSH) {
					return true;
				}
			}
		}
		return false;
	}

	private static void handleLily(ServerLevel level, BlockPos pos, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		if (!isStillWater(level.getBlockState(pos.below()))) {
			if (chance(random, BASE_DEATH * rates.death() * speed * 4.0)) {
				level.destroyBlock(pos, false);
				stats.flowerDeaths++;
			}
			return;
		}
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		if (!chance(random, BASE_SPREAD * rates.spread() * speed)) {
			return;
		}
		BlockPos target = pos.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
		if (!level.isEmptyBlock(target) || !isStillWater(level.getBlockState(target.below()))) {
			return;
		}
		if (!Blocks.LILY_PAD.defaultBlockState().canSurvive(level, target)) {
			return;
		}
		level.setBlock(target, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static void handleDeadBush(ServerLevel level, BlockPos pos, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		Block soil = level.getBlockState(pos.below()).getBlock();
		boolean drySoil = soil == Blocks.SAND || soil == Blocks.RED_SAND || soil == Blocks.TERRACOTTA || soil == Blocks.SUSPICIOUS_SAND;
		if (!drySoil || level.isRainingAt(pos)) {
			if (chance(random, BASE_DEATH * rates.death() * speed * (drySoil ? 1.0 : 4.0))) {
				level.destroyBlock(pos, false);
				stats.flowerDeaths++;
			}
			return;
		}
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		if (!chance(random, BASE_SPREAD * rates.spread() * speed)) {
			return;
		}
		BlockPos probe = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
		BlockPos ground = probe.below();
		Block groundBlock = level.getBlockState(ground).getBlock();
		if (groundBlock != Blocks.SAND && groundBlock != Blocks.RED_SAND && groundBlock != Blocks.TERRACOTTA) {
			return;
		}
		if (!level.isEmptyBlock(probe) || ForestEcology.isProtected(level.getBlockState(ground))) {
			return;
		}
		if (!Blocks.DEAD_BUSH.defaultBlockState().canSurvive(level, probe)) {
			return;
		}
		level.setBlock(probe, Blocks.DEAD_BUSH.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static boolean isStillWater(BlockState state) {
		return state.getBlock() == Blocks.WATER && (!(state.getBlock() instanceof LiquidBlock) || state.getFluidState().isSource());
	}

	private static void handleColumn(ServerLevel level, BlockPos pos, Block plant, int maxHeight, GroundCheck ground, BiomeRates rates, RandomSource random, double speed, TickStats stats, boolean dryOnly) {
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		BlockPos base = columnBase(level, pos, plant);
		if (!ground.test(level, base.below())) {
			if (chance(random, BASE_DEATH * rates.death() * speed * 4.0)) {
				level.destroyBlock(pos, true);
				stats.flowerDeaths++;
			}
			return;
		}
		if (dryOnly && level.isRainingAt(pos.above()) && chance(random, BASE_DEATH * rates.death() * speed)) {
			level.destroyBlock(pos, true);
			stats.flowerDeaths++;
			return;
		}
		int height = columnHeight(level, base, plant);
		if (height < maxHeight && chance(random, BASE_GROW * rates.growth() * speed)) {
			BlockPos top = base.above(height);
			if (level.isEmptyBlock(top) && plant.defaultBlockState().canSurvive(level, top)) {
				level.setBlock(top, plant.defaultBlockState(), Block.UPDATE_ALL);
				stats.flowerSpreads++;
				return;
			}
		}
		if (chance(random, BASE_SPREAD * rates.spread() * speed)) {
			BlockPos probe = pos.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			BlockPos soil = ForestEcology.walkToSoil(level, probe);
			if (soil == null) {
				soil = probe.below();
			}
			BlockPos air = soil.above();
			if (VillageZones.inVillage(level, air) || ForestEcology.isProtected(level.getBlockState(soil))) {
				return;
			}
			if (!ground.test(level, soil) || !level.isEmptyBlock(air)) {
				return;
			}
			if (!plant.defaultBlockState().canSurvive(level, air)) {
				return;
			}
			level.setBlock(air, plant.defaultBlockState(), Block.UPDATE_ALL);
			stats.flowerSpreads++;
		}
	}

	private static void handleMushroom(ServerLevel level, BlockPos pos, Block mushroom, BiomeRates rates, RandomSource random, double speed, TickStats stats) {
		if (level.getRawBrightness(pos, 0) > 12 && chance(random, BASE_DEATH * rates.death() * speed * 3.0)) {
			level.destroyBlock(pos, false);
			stats.flowerDeaths++;
			return;
		}
		if (VillageZones.inVillage(level, pos)) {
			return;
		}
		if (!chance(random, BASE_SPREAD * rates.spread() * speed)) {
			return;
		}
		BlockPos target = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
		BlockPos ground = target.below();
		Block soil = level.getBlockState(ground).getBlock();
		if (soil != Blocks.DIRT && soil != Blocks.GRASS_BLOCK && soil != Blocks.PODZOL && soil != Blocks.MYCELIUM && soil != Blocks.NETHERRACK && !level.getBlockState(ground).is(BlockTags.LOGS)) {
			return;
		}
		if (!level.isEmptyBlock(target) || level.getRawBrightness(target, 0) > 12) {
			return;
		}
		if (!mushroom.defaultBlockState().canSurvive(level, target)) {
			return;
		}
		level.setBlock(target, mushroom.defaultBlockState(), Block.UPDATE_ALL);
		stats.flowerSpreads++;
	}

	private static boolean cactusGround(ServerLevel level, BlockPos ground) {
		Block block = level.getBlockState(ground).getBlock();
		return block == Blocks.SAND || block == Blocks.RED_SAND;
	}

	private static boolean caneGround(ServerLevel level, BlockPos ground) {
		Block block = level.getBlockState(ground).getBlock();
		if (block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.SAND && block != Blocks.RED_SAND && block != Blocks.PODZOL && block != Blocks.MOSS_BLOCK) {
			return false;
		}
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			Block neighbor = level.getBlockState(ground.relative(dir)).getBlock();
			if (neighbor == Blocks.WATER || neighbor == Blocks.FROSTED_ICE) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos columnBase(ServerLevel level, BlockPos pos, Block plant) {
		BlockPos.MutableBlockPos cursor = pos.mutable();
		while (level.getBlockState(cursor.below()).getBlock() == plant) {
			cursor.move(0, -1, 0);
		}
		return cursor.immutable();
	}

	private static int columnHeight(ServerLevel level, BlockPos base, Block plant) {
		int height = 0;
		BlockPos.MutableBlockPos cursor = base.mutable();
		while (level.getBlockState(cursor).getBlock() == plant && height < 8) {
			height++;
			cursor.move(0, 1, 0);
		}
		return height;
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
