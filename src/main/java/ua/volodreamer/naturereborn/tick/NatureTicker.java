package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;
import ua.volodreamer.naturereborn.species.SpeciesRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Walks player-nearby loaded chunks with a per-tick budget.
 * Phase 2 samples the surface column plus one random block, then applies grass/tree actions.
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

		Set<ChunkPos> visited = new HashSet<>();
		int processedChunks = 0;

		for (ServerPlayer player : level.players()) {
			ChunkPos origin = player.chunkPosition();
			int originX = origin.getMinBlockX() >> 4;
			int originZ = origin.getMinBlockZ() >> 4;
			int radius = Math.max(0, config.playerFullRateDistanceChunks);
			for (int dz = -radius; dz <= radius && processedChunks < MAX_CHUNKS_PER_LEVEL_TICK; dz++) {
				for (int dx = -radius; dx <= radius && processedChunks < MAX_CHUNKS_PER_LEVEL_TICK; dx++) {
					int cx = originX + dx;
					int cz = originZ + dz;
					ChunkPos chunkPos = new ChunkPos(cx, cz);
					if (!visited.add(chunkPos) || !level.hasChunk(cx, cz)) {
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
		RandomSource random = level.getRandom();

		for (int i = 0; i < rolls; i++) {
			int x = pos.getMinBlockX() + random.nextInt(16);
			int z = pos.getMinBlockZ() + random.nextInt(16);
			int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			hit(level, chunk, new BlockPos(x, surfaceY - 1, z), config, stats);
			hit(level, chunk, new BlockPos(x, surfaceY, z), config, stats);
			int y = minY + random.nextInt(Math.max(1, height));
			hit(level, chunk, new BlockPos(x, y, z), config, stats);
		}
	}

	private static void hit(ServerLevel level, LevelChunk chunk, BlockPos blockPos, NatureRebornConfig config, TickStats stats) {
		if (!level.isInWorldBounds(blockPos)) {
			return;
		}
		stats.rolls++;
		BlockState state = chunk.getBlockState(blockPos);
		Species species = SpeciesRegistry.match(state.getBlock());
		if (species == null) {
			return;
		}
		stats.speciesHits++;
		BiomeRates rates = species.ratesFor(level, blockPos);
		stats.recordRateSample(species.id(), rates);
		NatureActions.apply(level, blockPos, state, species, rates, config, stats);
	}

	public static boolean isDimensionEnabled(ServerLevel level, NatureRebornConfig config) {
		if (level.dimension() == Level.OVERWORLD) {
			return config.overworldEnabled;
		}
		if (level.dimension() == Level.NETHER) {
			return config.netherEnabled;
		}
		return false;
	}
}
