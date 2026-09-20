package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;

final class VillageZones {
	private VillageZones() {
	}

	static boolean inVillage(ServerLevel level, BlockPos pos) {
		try {
			StructureStart start = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
			if (start != null && start.isValid()) {
				return true;
			}
		} catch (RuntimeException ignored) {
		}
		return looksLikeVillage(level, pos);
	}

	private static boolean looksLikeVillage(ServerLevel level, BlockPos pos) {
		int hits = 0;
		for (int dx = -6; dx <= 6; dx += 2) {
			for (int dz = -6; dz <= 6; dz += 2) {
				BlockPos probe = pos.offset(dx, 0, dz);
				BlockPos soil = ForestEcology.walkToSoil(level, probe);
				BlockPos at = soil != null ? soil : probe;
				BlockState state = level.getBlockState(at);
				Block block = state.getBlock();
				if (block == Blocks.DIRT_PATH || block == Blocks.FARMLAND || block == Blocks.BELL) {
					hits++;
				} else if (state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_STAIRS)) {
					hits++;
				} else if (block == Blocks.COBBLESTONE || block == Blocks.MOSSY_COBBLESTONE || block == Blocks.GLASS_PANE) {
					hits++;
				}
				BlockState above = level.getBlockState(at.above());
				if (above.is(BlockTags.WOODEN_DOORS) || above.is(BlockTags.BEDS) || above.getBlock() == Blocks.BELL) {
					hits += 2;
				}
			}
		}
		return hits >= 6;
	}
}
