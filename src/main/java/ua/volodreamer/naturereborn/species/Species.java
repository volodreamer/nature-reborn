package ua.volodreamer.naturereborn.species;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Species {
	private final String id;
	private final SpeciesKind kind;
	private final List<Block> blocks;
	private final Block plant;
	private final BiomeRates defaults;
	private final Map<Identifier, BiomeRates> biomeRates;
	private final Map<TagKey<Biome>, BiomeRates> biomeTagRates;

	public Species(
			String id,
			SpeciesKind kind,
			List<Block> blocks,
			Block plant,
			BiomeRates defaults,
			Map<Identifier, BiomeRates> biomeRates,
			Map<TagKey<Biome>, BiomeRates> biomeTagRates
	) {
		this.id = id;
		this.kind = kind;
		this.blocks = List.copyOf(blocks);
		this.plant = plant;
		this.defaults = defaults;
		this.biomeRates = Map.copyOf(biomeRates);
		this.biomeTagRates = new LinkedHashMap<>(biomeTagRates);
	}

	public String id() {
		return id;
	}

	public SpeciesKind kind() {
		return kind;
	}

	public List<Block> blocks() {
		return blocks;
	}

	public Block plant() {
		return plant;
	}

	public BiomeRates defaults() {
		return defaults;
	}

	public boolean matches(Block block) {
		return blocks.contains(block);
	}

	public BiomeRates ratesFor(Level level, BlockPos pos) {
		Holder<Biome> holder = level.getBiome(pos);
		Identifier biomeId = holder.unwrapKey().map(key -> key.identifier()).orElse(null);
		if (biomeId != null) {
			BiomeRates exact = biomeRates.get(biomeId);
			if (exact != null) {
				return exact;
			}
		}
		for (Map.Entry<TagKey<Biome>, BiomeRates> entry : biomeTagRates.entrySet()) {
			if (holder.is(entry.getKey())) {
				return entry.getValue();
			}
		}
		return defaults;
	}

	public static Block blockOrAir(String id) {
		if (id == null || id.isBlank()) {
			return Blocks.AIR;
		}
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	public static TagKey<Biome> biomeTag(String id) {
		return TagKey.create(Registries.BIOME, Identifier.parse(id));
	}
}
