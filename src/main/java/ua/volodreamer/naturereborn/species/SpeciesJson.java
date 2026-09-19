package ua.volodreamer.naturereborn.species;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpeciesJson {
	public String id;
	public String kind;
	public List<String> blocks = new ArrayList<>();
	public RateJson defaults = new RateJson();
	public Map<String, RateJson> biomes = new LinkedHashMap<>();

	public static final class RateJson {
		public Double growth;
		public Double spread;
		public Double death;

		public BiomeRates toRates(BiomeRates fallback) {
			double growth = this.growth != null ? this.growth : fallback.growth();
			double spread = this.spread != null ? this.spread : fallback.spread();
			double death = this.death != null ? this.death : fallback.death();
			return new BiomeRates(growth, spread, death);
		}
	}
}
