package ua.volodreamer.naturereborn.species;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import ua.volodreamer.naturereborn.NatureReborn;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpeciesLoader {
	private static final Gson GSON = new GsonBuilder().create();
	private static final ResourceLocation LISTENER_ID = ResourceLocation.fromNamespaceAndPath(NatureReborn.MOD_ID, "species");

	private SpeciesLoader() {
	}

	public static void register() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return LISTENER_ID;
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				load(manager);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> load(server.getResourceManager()));
	}

	static void load(ResourceManager manager) {
		List<Species> loaded = new ArrayList<>();
		Map<ResourceLocation, Resource> resources = manager.listResources(
				"naturereborn/species",
				path -> path.getPath().endsWith(".json")
		);
		for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
			try (Reader reader = entry.getValue().openAsReader()) {
				SpeciesJson json = GSON.fromJson(reader, SpeciesJson.class);
				Species species = parse(entry.getKey(), json);
				if (species != null) {
					loaded.add(species);
				}
			} catch (IOException | JsonParseException | IllegalArgumentException e) {
				NatureReborn.LOGGER.error("Failed to parse species {}", entry.getKey(), e);
			}
		}
		SpeciesRegistry.replaceAll(loaded);
	}

	private static Species parse(ResourceLocation file, SpeciesJson json) {
		if (json == null || json.id == null || json.id.isBlank()) {
			NatureReborn.LOGGER.error("Species file {} is missing id", file);
			return null;
		}
		SpeciesKind kind;
		try {
			kind = SpeciesKind.valueOf(json.kind == null ? "PLANT" : json.kind.toUpperCase());
		} catch (IllegalArgumentException e) {
			NatureReborn.LOGGER.error("Species {} has unknown kind {}", json.id, json.kind);
			return null;
		}

		List<Block> blocks = new ArrayList<>();
		for (String blockId : json.blocks) {
			Block block = BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse(blockId));
			if (block == Blocks.AIR) {
				NatureReborn.LOGGER.warn("Species {} references missing block {}", json.id, blockId);
				continue;
			}
			blocks.add(block);
		}
		if (blocks.isEmpty()) {
			NatureReborn.LOGGER.error("Species {} has no valid blocks", json.id);
			return null;
		}

		BiomeRates defaults = json.defaults.toRates(BiomeRates.NEUTRAL);
		Map<ResourceLocation, BiomeRates> biomeRates = new LinkedHashMap<>();
		Map<TagKey<Biome>, BiomeRates> tagRates = new LinkedHashMap<>();
		for (Map.Entry<String, SpeciesJson.RateJson> biome : json.biomes.entrySet()) {
			String key = biome.getKey();
			BiomeRates rates = biome.getValue().toRates(defaults);
			if (key.startsWith("#")) {
				tagRates.put(Species.biomeTag(key.substring(1)), rates);
			} else {
				biomeRates.put(ResourceLocation.parse(key), rates);
			}
		}
		return new Species(json.id, kind, blocks, defaults, biomeRates, tagRates);
	}
}
