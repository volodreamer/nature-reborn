package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

final class LeafLitter {
	private static final double BASE_PLACE = 0.01;
	private static final Block LITTER = litterBlock();

	private LeafLitter() {
	}

	private static Block litterBlock() {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:leaf_litter"));
		return block == Blocks.AIR ? Blocks.PINK_PETALS : block;
	}

	static boolean isLitter(Block block) {
		return block == LITTER;
	}

	static void tick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, double speed, TickStats stats) {
		if (VillageZones.inVillage(level, pos) || LITTER == Blocks.PINK_PETALS && !state.is(Blocks.PINK_PETALS)) {
			// still allow real leaf_litter when resolved
		}
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
		if (random.nextDouble() >= BASE_PLACE * speed * (1.0 - covered / Math.max(cap, 0.01))) {
			return;
		}

		BlockState existing = level.getBlockState(surface);
		if (isLitter(existing.getBlock())) {
			thicken(level, surface, existing);
			stats.grassRegrows++;
			return;
		}
		if (!existing.isAir() && !ForestEcology.isFoliage(existing.getBlock())) {
			return;
		}
		BlockState litter = LITTER.defaultBlockState();
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
		int max = amount.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(4);
		if (current < max) {
			level.setBlock(pos, state.setValue(amount, current + 1), Block.UPDATE_ALL);
		}
	}

	private static BlockState withSegments(BlockState state, int value) {
		IntegerProperty amount = segmentProperty(state);
		if (amount == null) {
			return state;
		}
		return state.setValue(amount, value);
	}

	private static IntegerProperty segmentProperty(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property instanceof IntegerProperty integer && ("segment_amount".equals(property.getName()) || "flower_amount".equals(property.getName()))) {
				return integer;
			}
		}
		return null;
	}

	private static double coverage(ServerLevel level, BlockPos soil) {
		int litter = 0;
		int samples = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				samples++;
				if (isLitter(level.getBlockState(soil.offset(dx, 1, dz)).getBlock())) {
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
		return 0;
	}
}
