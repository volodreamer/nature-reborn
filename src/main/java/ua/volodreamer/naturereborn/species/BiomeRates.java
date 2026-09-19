package ua.volodreamer.naturereborn.species;

public record BiomeRates(double growth, double spread, double death) {
	public static final BiomeRates NEUTRAL = new BiomeRates(1.0, 1.0, 1.0);

	public BiomeRates {
		growth = clamp(growth);
		spread = clamp(spread);
		death = clamp(death);
	}

	private static double clamp(double value) {
		if (Double.isNaN(value) || value < 0) {
			return 0;
		}
		return Math.min(value, 32.0);
	}
}
