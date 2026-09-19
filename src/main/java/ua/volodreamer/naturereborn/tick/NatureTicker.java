package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;
import ua.volodreamer.naturereborn.species.SpeciesRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Walks player-nearby loaded chunks with a per-tick budget.
 * Phase 1 only samples blocks and records stats; later phases act on hits.
 */
public final class NatureTicker {
	private static final int MAX_CHUNKS_PER_LEVEL_TICK = 48;

	private NatureTicker() {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(NatureTicker::onEndLevelTick);
	}

	private static void onEndLevelTick(ServerLevel level) {
		NatureRebornConfig config = NatureReborn.config();
		if (!config.masterEnabled || !isDimensionEnabled(level, config)) {
			return;
		}

		TickStats stats = TickStats.of(level);
		stats.beginTick();

		Set<Long> visited = new HashSet<>();
		int processedChunks = 0;

		for (ServerPlayer player : level.players()) {
			ChunkPos origin = player.chunkPosition();
			int radius = Math.max(0, config.playerFullRateDistanceChunks);
			for (int dz = -radius; dz <= radius && processedChunks < MAX_CHUNKS_PER_LEVEL_TICK; dz++) {
				for (int dx = -radius; dx <= radius && processedChunks < MAX_CHUNKS_PER_LEVEL_TICK; dx++) {
					int cx = origin.x + dx;
					int cz = origin.z + dz;
					long key = ChunkPos.asLong(cx, cz);
					if (!visited.add(key) || !level.hasChunk(cx, cz)) {
						continue;
					}
					LevelChunk chunk = level.getChunk(cx, cz);
					sampleChunk(level, chunk, config, stats);
					processedChunks++;
				}
			}
		}

		stats.endTick(processedChunks, visited.size());
	}

	private static void sampleChunk(ServerLevel level, LevelChunk chunk, NatureRebornConfig config, TickStats stats) {
		int rolls = Math.max(0, config.natureRollsPerChunkTick);
		ChunkPos pos = chunk.getPos();
		int minY = level.getMinY();
		int height = level.getHeight();

		for (int i = 0; i < rolls; i++) {
			int x = pos.getMinBlockX() + level.random.nextInt(16);
			int z = pos.getMinBlockZ() + level.random.nextInt(16);
			int y = minY + level.random.nextInt(Math.max(1, height));
			BlockPos blockPos = new BlockPos(x, y, z);
			stats.rolls++;

			Species species = SpeciesRegistry.match(chunk.getBlockState(blockPos).getBlock());
			if (species == null) {
				continue;
			}
			stats.speciesHits++;
			BiomeRates rates = species.ratesFor(level, blockPos);
			stats.recordRateSample(species.id(), rates);
		}
	}

	static boolean isDimensionEnabled(ServerLevel level, NatureRebornConfig config) {
		if (level.dimension() == Level.OVERWORLD) {
			return config.overworldEnabled;
		}
		if (level.dimension() == Level.NETHER) {
			return config.netherEnabled;
		}
		return false;
	}
}
