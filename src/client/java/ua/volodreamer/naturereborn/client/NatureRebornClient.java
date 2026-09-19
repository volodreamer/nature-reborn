package ua.volodreamer.naturereborn.client;

import net.fabricmc.api.ClientModInitializer;
import ua.volodreamer.naturereborn.NatureReborn;

public final class NatureRebornClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NatureReborn.LOGGER.info("Nature Reborn client ready");
	}
}
