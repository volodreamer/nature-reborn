package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ua.volodreamer.naturereborn.species.Species;

import java.util.ArrayList;
import java.util.List;

final class ForestEcology {
	static final int INTERIOR_RADIUS = 6;
	static final int INTERIOR_TRUNK_LIMIT = 2;
	static final int FALLEN_MIN_TRUNK = 8;
	static final int FALLEN_LENGTH_MIN = 3;
	static final int FALLEN_LENGTH_MAX = 4;
	static final double FALLEN_CHANCE = 0.06;

	private ForestEcology() {
	}

	static boolean needsTwoByTwo(Species species) {
		String id = species.id();
		return "dark_oak".equals(id) || "jungle".equals(id);
	}

	static boolean isTrunkBase(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
			return false;
		}
		return !level.getBlockState(pos.below()).is(BlockTags.LOGS);
	}

	static int trunkColumnHeight(ServerLevel level, BlockPos pos) {
		int height = 0;
		BlockPos.MutableBlockPos cursor = pos.mutable();
		while (cursor.getY() > level.getMinY() && level.getBlockState(cursor).is(BlockTags.LOGS)) {
			cursor.move(0, -1, 0);
		}
		cursor.move(0, 1, 0);
		while (cursor.getY() < level.getMinY() + level.getHeight() && level.getBlockState(cursor).is(BlockTags.LOGS)) {
			height++;
			cursor.move(0, 1, 0);
		}
		return height;
	}

	static int nearbyTrunks(ServerLevel level, BlockPos center, int radius) {
		int count = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos probe = center.offset(dx, 0, dz);
				BlockPos soil = walkToSoil(level, probe);
				if (soil == null) {
					continue;
				}
				BlockPos base = soil.above();
				if (isTrunkBase(level, base) && trunkColumnHeight(level, base) >= 3) {
					count++;
				}
			}
		}
		return count;
	}

	static boolean isInterior(ServerLevel level, BlockPos groundOrAir) {
		return nearbyTrunks(level, groundOrAir, INTERIOR_RADIUS) >= INTERIOR_TRUNK_LIMIT;
	}

	static double deathMultiplier(ServerLevel level, BlockPos logPos) {
		int height = trunkColumnHeight(level, logPos);
		boolean shaded = tallerNeighbor(level, logPos, height);
		if (height <= 4 && shaded) {
			return 3.5;
		}
		if (height <= 4) {
			return 1.4;
		}
		if (height >= 10) {
			return 0.04;
		}
		if (height >= 7) {
			return 0.08;
		}
		return 0.22;
	}

	private static boolean tallerNeighbor(ServerLevel level, BlockPos pos, int height) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				int other = trunkColumnHeight(level, pos.offset(dx, 0, dz));
				if (other >= height + 3) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean isTwoByTwoSapling(ServerLevel level, BlockPos pos) {
		Block sapling = level.getBlockState(pos).getBlock();
		if (!(sapling instanceof SaplingBlock)) {
			return false;
		}
		BlockPos[] origins = {
				pos,
				pos.west(),
				pos.north(),
				pos.west().north()
		};
		for (BlockPos origin : origins) {
			if (sameSapling(level, origin, sapling)
					&& sameSapling(level, origin.east(), sapling)
					&& sameSapling(level, origin.south(), sapling)
					&& sameSapling(level, origin.south().east(), sapling)) {
				return true;
			}
		}
		return false;
	}

	private static boolean sameSapling(ServerLevel level, BlockPos pos, Block sapling) {
		return level.getBlockState(pos).getBlock() == sapling;
	}

	static boolean tryPlaceTwoByTwo(ServerLevel level, BlockPos air, Block sapling) {
		BlockPos[] starts = {
				air,
				air.west(),
				air.north(),
				air.west().north()
		};
		for (BlockPos start : starts) {
			if (canFitTwoByTwo(level, start, sapling)) {
				placeTwoByTwo(level, start, sapling);
				return true;
			}
		}
		return false;
	}

	private static boolean canFitTwoByTwo(ServerLevel level, BlockPos origin, Block sapling) {
		BlockPos[] cells = {
				origin,
				origin.east(),
				origin.south(),
				origin.south().east()
		};
		for (BlockPos cell : cells) {
			BlockPos ground = cell.below();
			if (!isPlantableSoil(level.getBlockState(ground).getBlock()) || isProtected(level.getBlockState(ground))) {
				return false;
			}
			BlockState above = level.getBlockState(cell);
			if (!above.isAir() && above.getBlock() != sapling && !isFoliage(above.getBlock())) {
				return false;
			}
		}
		return true;
	}

	private static void placeTwoByTwo(ServerLevel level, BlockPos origin, Block sapling) {
		BlockState state = sapling.defaultBlockState();
		level.setBlock(origin, state, Block.UPDATE_ALL);
		level.setBlock(origin.east(), state, Block.UPDATE_ALL);
		level.setBlock(origin.south(), state, Block.UPDATE_ALL);
		level.setBlock(origin.south().east(), state, Block.UPDATE_ALL);
	}

	static Direction leanDirection(BlockPos root, List<BlockPos> logs) {
		int east = 0;
		int west = 0;
		int south = 0;
		int north = 0;
		for (BlockPos log : logs) {
			int dx = log.getX() - root.getX();
			int dz = log.getZ() - root.getZ();
			if (dx > 0) {
				east += dx;
			} else if (dx < 0) {
				west -= dx;
			}
			if (dz > 0) {
				south += dz;
			} else if (dz < 0) {
				north -= dz;
			}
		}
		int best = east;
		Direction dir = Direction.EAST;
		if (west > best) {
			best = west;
			dir = Direction.WEST;
		}
		if (south > best) {
			best = south;
			dir = Direction.SOUTH;
		}
		if (north > best) {
			dir = Direction.NORTH;
		}
		return dir;
	}

	static void placeFallenLog(ServerLevel level, BlockPos root, List<BlockPos> logs, BlockState sample, RandomSource random) {
		int trunk = 0;
		for (BlockPos log : logs) {
			if (log.getX() == root.getX() && log.getZ() == root.getZ()) {
				trunk++;
			}
		}
		if (trunk < FALLEN_MIN_TRUNK) {
			return;
		}
		if (random.nextDouble() >= FALLEN_CHANCE) {
			return;
		}
		Direction dir = leanDirection(root, logs);
		Direction.Axis axis = dir.getAxis();
		BlockState fallen = sample.hasProperty(BlockStateProperties.AXIS)
				? sample.setValue(BlockStateProperties.AXIS, axis)
				: sample;
		int length = FALLEN_LENGTH_MIN + random.nextInt(FALLEN_LENGTH_MAX - FALLEN_LENGTH_MIN + 1);
		BlockPos cursor = root.relative(dir);
		for (int i = 0; i < length; i++) {
			BlockPos ground = walkToSoil(level, cursor);
			BlockPos place = ground != null ? ground.above() : cursor;
			if (place.equals(root)) {
				cursor = cursor.relative(dir);
				continue;
			}
			if (level.isEmptyBlock(place) || isFoliage(level.getBlockState(place).getBlock())) {
				level.setBlock(place, fallen, Block.UPDATE_ALL);
			}
			cursor = cursor.relative(dir);
		}
	}

	static void relocateHives(ServerLevel level, List<BlockPos> hives, RandomSource random) {
		for (BlockPos hive : hives) {
			BlockState state = level.getBlockState(hive);
			if (!isHive(state.getBlock())) {
				continue;
			}
			level.removeBlock(hive, false);
			if (random.nextFloat() < 0.4f) {
				continue;
			}
			BlockPos dest = findHiveSupport(level, hive);
			if (dest != null) {
				level.setBlock(dest, state, Block.UPDATE_ALL);
			}
		}
	}

	private static BlockPos findHiveSupport(ServerLevel level, BlockPos from) {
		for (int dy = 0; dy < 20; dy++) {
			BlockPos probe = from.below(dy);
			BlockState state = level.getBlockState(probe);
			if (state.isAir() || isFoliage(state.getBlock()) || isHive(state.getBlock()) || state.is(BlockTags.LEAVES)) {
				continue;
			}
			BlockPos above = probe.above();
			if (level.isEmptyBlock(above) || isFoliage(level.getBlockState(above).getBlock())) {
				return above;
			}
		}
		return null;
	}

	static boolean isHive(Block block) {
		return block == Blocks.BEE_NEST || block == Blocks.BEEHIVE;
	}

	static List<BlockPos> collectAdjacentHives(ServerLevel level, List<BlockPos> parts) {
		List<BlockPos> hives = new ArrayList<>();
		for (BlockPos part : parts) {
			for (Direction direction : Direction.values()) {
				BlockPos next = part.relative(direction);
				if (isHive(level.getBlockState(next).getBlock()) && !hives.contains(next)) {
					hives.add(next);
				}
			}
			if (isHive(level.getBlockState(part).getBlock()) && !hives.contains(part)) {
				hives.add(part);
			}
		}
		return hives;
	}

	static void clearOrphanLogs(ServerLevel level, BlockPos root, Species species) {
		for (int dx = -10; dx <= 10; dx++) {
			for (int dz = -10; dz <= 10; dz++) {
				for (int dy = -2; dy <= 28; dy++) {
					BlockPos pos = root.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (!state.is(BlockTags.LOGS) || !species.matches(state.getBlock())) {
						continue;
					}
					if (!connectedToGround(level, pos)) {
						level.destroyBlock(pos, false);
					}
				}
			}
		}
	}

	private static boolean connectedToGround(ServerLevel level, BlockPos start) {
		BlockPos.MutableBlockPos cursor = start.mutable();
		for (int i = 0; i < 32; i++) {
			BlockState state = level.getBlockState(cursor);
			if (!state.is(BlockTags.LOGS)) {
				return isPlantableSoil(state.getBlock()) || !state.isAir();
			}
			cursor.move(0, -1, 0);
		}
		return false;
	}

	static BlockPos edgePlantSpot(ServerLevel level, BlockPos root, RandomSource random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			int dist = 5 + random.nextInt(4);
			Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			BlockPos probe = root.relative(dir, dist);
			BlockPos soil = walkToSoil(level, probe);
			if (soil == null || isProtected(level.getBlockState(soil))) {
				continue;
			}
			BlockPos air = soil.above();
			if (!level.isEmptyBlock(air) && !isFoliage(level.getBlockState(air).getBlock())) {
				continue;
			}
			if (level.getRawBrightness(air, 0) < 8) {
				continue;
			}
			if (isInterior(level, air)) {
				continue;
			}
			return air;
		}
		return null;
	}

	static BlockPos walkToSoil(ServerLevel level, BlockPos start) {
		BlockPos.MutableBlockPos cursor = start.mutable();
		for (int i = 0; i < 20; i++) {
			Block block = level.getBlockState(cursor).getBlock();
			if (isPlantableSoil(block) || isProtected(level.getBlockState(cursor))) {
				return cursor.immutable();
			}
			cursor.move(0, -1, 0);
		}
		return null;
	}

	static boolean isPlantableSoil(Block block) {
		return block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.PODZOL
				|| block == Blocks.MOSS_BLOCK
				|| block == Blocks.ROOTED_DIRT;
	}

	static boolean isProtected(BlockState state) {
		Block block = state.getBlock();
		return block == Blocks.DIRT_PATH || block == Blocks.FARMLAND;
	}

	static boolean isFoliage(Block block) {
		return block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.TALL_GRASS || block == Blocks.LARGE_FERN;
	}
}
