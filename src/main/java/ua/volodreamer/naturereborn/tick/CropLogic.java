package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;

public final class CropLogic {
	private static final double BASE_SENESCE = 0.006;
	private static final double BASE_GROW = 0.1;
	private static final int MIN_LIGHT = 9;
	private static final int SPREAD_TRIES = 4;

	private CropLogic() {
	}

	public static void tick(ServerLevel level, BlockPos pos, BlockState state, Species species, BiomeRates rates, NatureRebornConfig config, TickStats stats) {
		if (!config.cropsEnabled) {
			return;
		}
		RandomSource random = level.getRandom();
		double speed = config.clampedSpeed();
		if (speed <= 0) {
			return;
		}

		if (!isMature(state)) {
			if (chance(random, BASE_GROW * rates.growth() * speed)) {
				growOneStage(level, pos, state);
				stats.cropGrowths++;
			}
			return;
		}

		int attempts = Math.max(1, (int) Math.ceil(SPREAD_TRIES * Math.min(speed, 3.0) * Math.max(0.35, rates.spread())));
		boolean spread = false;
		for (int i = 0; i < attempts && !spread; i++) {
			spread = trySpread(level, pos, species, config, random, stats);
		}

		if (config.cropSenescenceEnabled && spread && chance(random, BASE_SENESCE * rates.death() * speed)) {
			senesce(level, pos, state, species, config, random, stats);
		}
	}

	public static boolean isMature(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if (block instanceof NetherWartBlock) {
			return state.hasProperty(BlockStateProperties.AGE_3) && state.getValue(BlockStateProperties.AGE_3) >= 3;
		}
		if (state.hasProperty(BlockStateProperties.AGE_7)) {
			return state.getValue(BlockStateProperties.AGE_7) >= 7;
		}
		return false;
	}

	public static boolean isCropBlock(Block block) {
		return block instanceof CropBlock || block instanceof NetherWartBlock || block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM;
	}

	public static BlockState seedState(Block crop) {
		BlockState state = crop.defaultBlockState();
		if (state.hasProperty(BlockStateProperties.AGE_7)) {
			return state.setValue(BlockStateProperties.AGE_7, 0);
		}
		if (state.hasProperty(BlockStateProperties.AGE_3)) {
			return state.setValue(BlockStateProperties.AGE_3, 0);
		}
		return state;
	}

	private static void growOneStage(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			level.scheduleTick(pos, crop, 1);
			return;
		}
		if (state.hasProperty(BlockStateProperties.AGE_7)) {
			int age = state.getValue(BlockStateProperties.AGE_7);
			if (age < 7) {
				level.setBlock(pos, state.setValue(BlockStateProperties.AGE_7, age + 1), Block.UPDATE_ALL);
			}
			return;
		}
		if (state.hasProperty(BlockStateProperties.AGE_3)) {
			int age = state.getValue(BlockStateProperties.AGE_3);
			if (age < 3) {
				level.setBlock(pos, state.setValue(BlockStateProperties.AGE_3, age + 1), Block.UPDATE_ALL);
			}
		}
	}

	private static void senesce(ServerLevel level, BlockPos pos, BlockState state, Species species, NatureRebornConfig config, RandomSource random, TickStats stats) {
		level.destroyBlock(pos, true);
		stats.cropDeaths++;
		Block crop = state.getBlock();
		if (canPlantAt(level, pos, crop, config)) {
			level.setBlock(pos, seedState(crop), Block.UPDATE_ALL);
			stats.cropReplants++;
		}
		trySpread(level, pos, species, config, random, stats);
	}

	private static boolean trySpread(ServerLevel level, BlockPos origin, Species species, NatureRebornConfig config, RandomSource random, TickStats stats) {
		Block crop = cropBlock(species);
		int radius = Math.max(1, config.cropSpreadRadius);
		for (int attempt = 0; attempt < 8; attempt++) {
			int dx = random.nextInt(radius * 2 + 1) - radius;
			int dz = random.nextInt(radius * 2 + 1) - radius;
			if (dx == 0 && dz == 0) {
				continue;
			}
			int[] heights = {0, 1, -1};
			shuffle(heights, random);
			for (int dy : heights) {
				BlockPos target = origin.offset(dx, dy, dz);
				BlockPos ground = target.below();
				if (!isSoil(level.getBlockState(ground).getBlock())) {
					continue;
				}
				if (blockedByBarrier(level, origin, target)) {
					continue;
				}
				BlockState cover = level.getBlockState(target);
				if (!canOccupy(cover, random)) {
					continue;
				}
				if (!prepareSoil(level, ground, crop, config)) {
					continue;
				}
				if (!isNetherCrop(crop) && level.getRawBrightness(target, 0) < MIN_LIGHT) {
					continue;
				}
				if (cover.getBlock() instanceof DoublePlantBlock) {
					level.removeBlock(target.above(), false);
				}
				level.setBlock(target, seedState(crop), Block.UPDATE_ALL);
				stats.cropSpreads++;
				return true;
			}
		}
		return false;
	}

	private static void shuffle(int[] values, RandomSource random) {
		for (int i = values.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int tmp = values[i];
			values[i] = values[j];
			values[j] = tmp;
		}
	}

	private static boolean isSoil(Block block) {
		return block == Blocks.FARMLAND
				|| block == Blocks.GRASS_BLOCK
				|| block == Blocks.DIRT
				|| block == Blocks.COARSE_DIRT
				|| block == Blocks.ROOTED_DIRT
				|| block == Blocks.SOUL_SAND
				|| block == Blocks.SOUL_SOIL;
	}

	static boolean blockedByBarrier(ServerLevel level, BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		int steps = Math.max(Math.abs(dx), Math.abs(dz));
		if (steps <= 0) {
			return isBarrier(level.getBlockState(from)) || isBarrier(level.getBlockState(from.below()));
		}
		for (int i = 1; i <= steps; i++) {
			int x = from.getX() + dx * i / steps;
			int z = from.getZ() + dz * i / steps;
			BlockPos mid = new BlockPos(x, from.getY(), z);
			if (isBarrier(level.getBlockState(mid)) || isBarrier(level.getBlockState(mid.below())) || isBarrier(level.getBlockState(mid.above()))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isBarrier(BlockState state) {
		return state.is(BlockTags.FENCES)
				|| state.is(BlockTags.FENCE_GATES)
				|| state.is(BlockTags.WALLS)
				|| state.getBlock() == Blocks.IRON_BARS;
	}

	private static boolean canOccupy(BlockState cover, RandomSource random) {
		if (cover.isAir()) {
			return true;
		}
		Block block = cover.getBlock();
		boolean soft = ForestEcology.isFoliage(block)
				|| block instanceof FlowerBlock
				|| block instanceof TallFlowerBlock
				|| cover.is(BlockTags.FLOWERS)
				|| cover.is(BlockTags.SMALL_FLOWERS)
				|| block == Blocks.PINK_PETALS;
		if (!soft) {
			return false;
		}
		return random.nextBoolean();
	}

	private static Block cropBlock(Species species) {
		if (!species.blocks().isEmpty()) {
			return species.blocks().getFirst();
		}
		return Blocks.WHEAT;
	}

	public static boolean canPlantAt(ServerLevel level, BlockPos cropPos, Block crop, NatureRebornConfig config) {
		return prepareSoil(level, cropPos.below(), crop, config) && (level.isEmptyBlock(cropPos) || level.getBlockState(cropPos).getBlock() == crop);
	}

	public static boolean prepareSoil(ServerLevel level, BlockPos ground, Block crop, NatureRebornConfig config) {
		Block soil = level.getBlockState(ground).getBlock();
		if (isNetherCrop(crop)) {
			return soil == Blocks.SOUL_SAND || soil == Blocks.SOUL_SOIL;
		}
		if (soil == Blocks.FARMLAND) {
			return true;
		}
		if (!config.cropSpreadConvertsSoil) {
			return false;
		}
		if (soil == Blocks.DIRT || soil == Blocks.GRASS_BLOCK || soil == Blocks.COARSE_DIRT || soil == Blocks.ROOTED_DIRT) {
			level.setBlock(ground, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
			return true;
		}
		return false;
	}

	public static boolean isNetherCrop(Block crop) {
		return crop == Blocks.NETHER_WART;
	}

	public static Item seedItem(Block crop) {
		if (crop == Blocks.WHEAT) {
			return Items.WHEAT_SEEDS;
		}
		if (crop == Blocks.CARROTS) {
			return Items.CARROT;
		}
		if (crop == Blocks.POTATOES) {
			return Items.POTATO;
		}
		if (crop == Blocks.BEETROOTS) {
			return Items.BEETROOT_SEEDS;
		}
		if (crop == Blocks.NETHER_WART) {
			return Items.NETHER_WART;
		}
		if (crop == Blocks.MELON_STEM || crop == Blocks.ATTACHED_MELON_STEM) {
			return Items.MELON_SEEDS;
		}
		if (crop == Blocks.PUMPKIN_STEM || crop == Blocks.ATTACHED_PUMPKIN_STEM) {
			return Items.PUMPKIN_SEEDS;
		}
		return Items.WHEAT_SEEDS;
	}

	public static boolean consumeSeed(Player player, Item seed) {
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(seed)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
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
