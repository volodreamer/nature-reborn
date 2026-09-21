package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class GroundCover {
	private GroundCover() {
	}

	static Block pick(ServerLevel level, BlockPos pos, RandomSource random) {
		Holder<Biome> biome = level.getBiome(pos);
		String id = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		if (id.contains("desert") || id.contains("badlands")) {
			return random.nextFloat() < 0.25f ? Blocks.DEAD_BUSH : null;
		}
		if (id.contains("flower_forest") || id.contains("meadow") || id.contains("sunflower")) {
			if (random.nextFloat() < 0.55f) {
				return flower(id, random);
			}
			return Blocks.SHORT_GRASS;
		}
		if (id.contains("swamp") || id.contains("taiga") || id.contains("old_growth")) {
			return random.nextFloat() < 0.7f ? Blocks.FERN : Blocks.SHORT_GRASS;
		}
		if (id.contains("jungle")) {
			return random.nextFloat() < 0.6f ? Blocks.FERN : Blocks.SHORT_GRASS;
		}
		if (id.contains("savanna")) {
			return Blocks.SHORT_GRASS;
		}
		if (id.contains("plains")) {
			if (random.nextFloat() < 0.2f) {
				return random.nextBoolean() ? Blocks.DANDELION : Blocks.POPPY;
			}
			return Blocks.SHORT_GRASS;
		}
		if (id.contains("forest")) {
			return random.nextFloat() < 0.35f ? Blocks.FERN : Blocks.SHORT_GRASS;
		}
		return Blocks.SHORT_GRASS;
	}

	private static Block flower(String biomePath, RandomSource random) {
		if (biomePath.contains("sunflower") && random.nextFloat() < 0.15f) {
			return Blocks.DANDELION;
		}
		Block[] flowers = {
				Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
				Blocks.AZURE_BLUET, Blocks.ALLIUM
		};
		return flowers[random.nextInt(flowers.length)];
	}
}
