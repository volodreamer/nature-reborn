package ua.volodreamer.naturereborn.tick;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import ua.volodreamer.naturereborn.species.BiomeRates;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TickStats {
	private static final Map<ResourceKey<Level>, TickStats> BY_LEVEL = new ConcurrentHashMap<>();

	public int rolls;
	public int speciesHits;
	public int grassSpreads;
	public int grassRegrows;
	public int grassDeaths;
	public int saplingsPlanted;
	public int chunksProcessedLastTick;
	public int uniqueChunkKeysConsidered;
	public long lastTickNanos;
	public String lastHitSpecies = "-";
	public BiomeRates lastRates = BiomeRates.NEUTRAL;

	private long tickStartNanos;

	public static TickStats of(ServerLevel level) {
		return BY_LEVEL.computeIfAbsent(level.dimension(), key -> new TickStats());
	}

	public static TickStats of(ResourceKey<Level> key) {
		return BY_LEVEL.get(key);
	}

	public void beginTick() {
		rolls = 0;
		speciesHits = 0;
		grassSpreads = 0;
		grassRegrows = 0;
		grassDeaths = 0;
		saplingsPlanted = 0;
		tickStartNanos = System.nanoTime();
	}

	public void endTick(int chunksProcessed, int uniqueKeys) {
		chunksProcessedLastTick = chunksProcessed;
		uniqueChunkKeysConsidered = uniqueKeys;
		lastTickNanos = System.nanoTime() - tickStartNanos;
	}

	public void recordRateSample(String speciesId, BiomeRates rates) {
		lastHitSpecies = speciesId;
		lastRates = rates;
	}

	public String summaryLine() {
		return String.format(Locale.ROOT,
				"chunks=%d rolls=%d hits=%d grass[s=%d r=%d d=%d] saplings=%d last=%s rates[g=%.2f s=%.2f d=%.2f] cost=%.3fms",
				chunksProcessedLastTick,
				rolls,
				speciesHits,
				grassSpreads,
				grassRegrows,
				grassDeaths,
				saplingsPlanted,
				lastHitSpecies,
				lastRates.growth(),
				lastRates.spread(),
				lastRates.death(),
				lastTickNanos / 1_000_000.0);
	}
}
