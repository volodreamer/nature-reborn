package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

/**
 * Budgeted per-world hook. Phase 0 only records that the server is ticking;
 * plant/tree/crop scanners attach here later.
 */
public final class NatureTicker {
	private NatureTicker() {
	}

	public static void register(ServerTickEvents.EndWorldTick unusedSignatureHolder) {
		ServerTickEvents.END_WORLD_TICK.register(NatureTicker::onEndWorldTick);
	}

	private static void onEndWorldTick(ServerLevel level) {
		NatureRebornConfig config = NatureReborn.config();
		if (!config.masterEnabled) {
			return;
		}
		if (level.dimension() == Level.OVERWORLD && !config.overworldEnabled) {
			return;
		}
		if (level.dimension() == Level.NETHER && !config.netherEnabled) {
			return;
		}
		if (level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER) {
			return;
		}
		// Later: walk loaded chunks with a per-tick budget.
	}
}
