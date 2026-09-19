package ua.volodreamer.naturereborn;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.volodreamer.naturereborn.command.NatureRebornCommands;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.SpeciesLoader;
import ua.volodreamer.naturereborn.tick.AutoReplant;
import ua.volodreamer.naturereborn.tick.NatureTicker;

public final class NatureReborn implements ModInitializer {
	public static final String MOD_ID = "naturereborn";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static NatureRebornConfig config = NatureRebornConfig.defaults();

	@Override
	public void onInitialize() {
		config = NatureRebornConfig.load();
		SpeciesLoader.register();
		NatureTicker.register();
		AutoReplant.register();
		NatureRebornCommands.register();
		LOGGER.info("Nature Reborn {} loaded (master={}, overworld={}, nether={})",
				config.versionTag(),
				config.masterEnabled,
				config.overworldEnabled,
				config.netherEnabled);
	}

	public static NatureRebornConfig config() {
		return config;
	}

	public static void setConfig(NatureRebornConfig next) {
		config = next;
		NatureRebornConfig.save(next);
	}
}
