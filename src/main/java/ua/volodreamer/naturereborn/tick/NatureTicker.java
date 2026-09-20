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
import java.util.List;
import java.util.Set;

public final class NatureTicker {
	private static final int MAX_CHUNKS_PER_LEVEL_TICK = 24;

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

		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			stats.endTick(0, 0);
			return;
		}

		RandomSource random = level.getRandom();
		int radius = Math.max(0, config.playerFullRateDistanceChunks);
		Set<ChunkPos> visited = new HashSet<>();
		int processedChunks = 0;
		int attempts = MAX_CHUNKS_PER_LEVEL_TICK * 3;

		for (int i = 0; i < attempts && processedChunks < MAX_CHUNKS_PER_LEVEL_TICK; i++) {
			ServerPlayer player = players.get(random.nextInt(players.size()));
			ChunkPos origin = player.chunkPosition();
			int originX = origin.getMinBlockX() >> 4;
			int originZ = origin.getMinBlockZ() >> 4;
			int cx = originX + random.nextInt(radius * 2 + 1) - radius;
			int cz = originZ + random.nextInt(radius * 2 + 1) - radius;
			ChunkPos chunkPos = new ChunkPos(cx, cz);
			if (!visited.add(chunkPos) || !level.hasChunk(cx, cz)) {
				continue;
			}
			LevelChunk chunk = level.getChunk(cx, cz);
			sampleChunk(level, chunk, config, stats);
			processedChunks++;
		}

		stats.endTick(processedChunks, visited.size());
	}

	private static void sampleChunk(ServerLevel level, LevelChunk chunk, NatureRebornConfig config, TickStats stats) {
		int rolls = Math.max(0, config.natureRollsPerChunkTick);
		ChunkPos pos = chunk.getPos();
		RandomSource random = level.getRandom();

		for (int i = 0; i < rolls; i++) {
			int x = pos.getMinBlockX() + random.nextInt(16);
			int z = pos.getMinBlockZ() + random.nextInt(16);
			int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			int canopyY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			hit(level, chunk, new BlockPos(x, groundY - 1, z), config, stats);
			hit(level, chunk, new BlockPos(x, groundY, z), config, stats);
			hit(level, chunk, new BlockPos(x, groundY + 1, z), config, stats);
			if (canopyY > groundY) {
				int leafY = groundY + 1 + random.nextInt(Math.max(1, canopyY - groundY));
				hit(level, chunk, new BlockPos(x, leafY, z), config, stats);
			}
		}
	}

	private static void hit(ServerLevel level, LevelChunk chunk, BlockPos blockPos, NatureRebornConfig config, TickStats stats) {
		if (!level.isInWorldBounds(blockPos)) {
			return;
		}
		stats.rolls++;
		BlockState state = chunk.getBlockState(blockPos);
		if (FireLogic.isFire(state)) {
			FireLogic.tick(level, blockPos, state, config);
			return;
		}
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
