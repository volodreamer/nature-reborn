package ua.volodreamer.naturereborn.species;

import net.minecraft.world.level.block.Block;
import ua.volodreamer.naturereborn.NatureReborn;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpeciesRegistry {
	private static final Map<String, Species> BY_ID = new LinkedHashMap<>();
	private static final Map<Block, Species> BY_BLOCK = new LinkedHashMap<>();

	private SpeciesRegistry() {
	}

	public static void replaceAll(Collection<Species> species) {
		BY_ID.clear();
		BY_BLOCK.clear();
		for (Species entry : species) {
			BY_ID.put(entry.id(), entry);
			for (Block block : entry.blocks()) {
				Species previous = BY_BLOCK.put(block, entry);
				if (previous != null) {
					NatureReborn.LOGGER.warn("Block {} mapped to {} replaces {}", block, entry.id(), previous.id());
				}
			}
		}
		NatureReborn.LOGGER.info("Loaded {} nature species ({} block bindings)", BY_ID.size(), BY_BLOCK.size());
	}

	public static Species match(Block block) {
		return BY_BLOCK.get(block);
	}

	public static Species byId(String id) {
		return BY_ID.get(id);
	}

	public static Collection<Species> all() {
		return BY_ID.values();
	}

	public static int size() {
		return BY_ID.size();
	}
}
