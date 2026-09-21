package ua.volodreamer.naturereborn.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ua.volodreamer.naturereborn.NatureReborn;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NatureRebornConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int SCHEMA = 3;

	public int configSchema = SCHEMA;

	public boolean masterEnabled = true;

	public boolean overworldEnabled = true;
	public boolean netherEnabled = true;

	public double natureSpeed = 1.0;

	public boolean plantsEnabled = true;
	public boolean treesEnabled = true;
	public boolean grassEnabled = true;
	public boolean cropsEnabled = true;
	public boolean cropSpreadConvertsSoil = true;
	public boolean cropSenescenceEnabled = true;
	public double cropMatureLifeMultiplier = 3.0;
	public int cropSpreadRadius = 3;

	public boolean autoReplantEnabled = true;
	public boolean autoReplantAllSources = true;

	public boolean animalBreedingEnabled = true;

	public boolean lumberjackEnabled = false;
	public boolean lumberjackSneakBypass = true;
	public int lumberjackMaxLogs = 320;
	public int lumberjackMaxLeaves = 1400;

	public boolean fireEnabled = true;
	public boolean fireBurnUntilConsumed = false;

	public boolean fallenLogsEnabled = true;
	public boolean weatheringEnabled = true;

	public int playerFullRateDistanceChunks = 8;
	public int natureRollsPerChunkTick = 4;

	public static NatureRebornConfig defaults() {
		return new NatureRebornConfig();
	}

	public String versionTag() {
		return "0.1.0-alpha";
	}

	public double clampedSpeed() {
		if (Double.isNaN(natureSpeed) || natureSpeed < 0) {
			return 0;
		}
		return Math.min(natureSpeed, 8.0);
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("naturereborn.json");
	}

	public static NatureRebornConfig load() {
		Path file = path();
		if (!Files.exists(file)) {
			NatureRebornConfig created = defaults();
			save(created);
			return created;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			NatureRebornConfig loaded = GSON.fromJson(reader, NatureRebornConfig.class);
			if (loaded == null) {
				return defaults();
			}
			if (loaded.configSchema < 2) {
				loaded.lumberjackEnabled = false;
			}
			if (loaded.configSchema < SCHEMA) {
				loaded.configSchema = SCHEMA;
				save(loaded);
			}
			return loaded;
		} catch (IOException e) {
			NatureReborn.LOGGER.error("Failed to read config, using defaults", e);
			return defaults();
		}
	}

	public static void save(NatureRebornConfig config) {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			NatureReborn.LOGGER.error("Failed to write config", e);
		}
	}
}
