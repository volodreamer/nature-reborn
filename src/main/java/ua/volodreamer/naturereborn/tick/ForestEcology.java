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

import java.util.List;

final class ForestEcology {
	static final int INTERIOR_RADIUS = 5;
	static final int INTERIOR_TRUNK_LIMIT = 2;
	static final int FALLEN_MIN_TRUNK = 7;
	static final int FALLEN_LENGTH_MIN = 3;
	static final int FALLEN_LENGTH_MAX = 4;
	static final double FALLEN_CHANCE = 0.18;

	private ForestEcology() {
	}

	static boolean needsTwoByTwo(Species species) {
		String id = species.id();
		return "dark_oak".equals(id) || "jungle".equals(id);
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
				if (level.getBlockState(soil.above()).is(BlockTags.LOGS)) {
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
		BlockPos top = logPos.atY(logPos.getY() + Math.max(0, height - 1));
		int light = level.getRawBrightness(top.above(), 0);
		boolean shaded = light < 9 || tallerNeighbor(level, logPos, height);
		if (height <= 5 && shaded) {
			return 4.2;
		}
		if (height <= 5) {
			return 1.6;
		}
		if (height >= 8 && !shaded) {
			return 0.45;
		}
		return 1.0;
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

	static boolean tryPlaceTwoByTwo(ServerLevel level, BlockPos air, Block sapling) {
		BlockPos[] cells = {
				air,
				air.east(),
				air.south(),
				air.south().east()
		};
		for (BlockPos cell : cells) {
			BlockPos ground = cell.below();
			if (!isPlantableSoil(level.getBlockState(ground).getBlock())) {
				return false;
			}
			if (isProtected(level.getBlockState(ground))) {
				return false;
			}
			BlockState above = level.getBlockState(cell);
			if (!above.isAir() && !(above.getBlock() instanceof SaplingBlock) && !isFoliage(above.getBlock())) {
				return false;
			}
			if (level.getRawBrightness(cell, 0) < 9) {
				return false;
			}
		}
		BlockState state = sapling.defaultBlockState();
		for (BlockPos cell : cells) {
			level.setBlock(cell, state, Block.UPDATE_ALL);
		}
		return true;
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
		BlockPos cursor = root;
		for (int i = 0; i < length; i++) {
			BlockPos ground = walkToSoil(level, cursor);
			BlockPos place = ground != null ? ground.above() : cursor;
			if (level.isEmptyBlock(place) || isFoliage(level.getBlockState(place).getBlock())) {
				level.setBlock(place, fallen, Block.UPDATE_ALL);
			}
			cursor = cursor.relative(dir);
		}
	}

	static BlockPos edgePlantSpot(ServerLevel level, BlockPos root, RandomSource random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			int dist = 4 + random.nextInt(3);
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
			if (level.getRawBrightness(air, 0) < 9) {
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
		for (int i = 0; i < 12; i++) {
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
