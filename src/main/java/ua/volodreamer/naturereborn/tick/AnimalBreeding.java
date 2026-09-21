package ua.volodreamer.naturereborn.tick;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AnimalBreeding {
	private static final double PAIR_RANGE = 8.0;
	private static final double CLUSTER_RANGE = 16.0;
	private static final double SCAN_RANGE = 48.0;
	private static final double BASE_CHANCE = 0.08;

	private AnimalBreeding() {
	}

	static void tick(ServerLevel level, List<ServerPlayer> players, NatureRebornConfig config) {
		if (!config.animalBreedingEnabled || players.isEmpty()) {
			return;
		}
		double speed = config.clampedSpeed();
		if (speed <= 0) {
			return;
		}
		RandomSource random = level.getRandom();
		Set<Integer> seen = new HashSet<>();
		for (ServerPlayer player : players) {
			AABB scan = player.getBoundingBox().inflate(SCAN_RANGE);
			List<Animal> animals = level.getEntitiesOfClass(Animal.class, scan, AnimalBreeding::supported);
			for (Animal animal : animals) {
				if (!seen.add(animal.getId())) {
					continue;
				}
				if (animal.isBaby() || animal.getAge() != 0) {
					continue;
				}
				if (random.nextDouble() >= BASE_CHANCE * Math.min(speed, 3.0)) {
					continue;
				}
				tryBreedCluster(level, animal, random, seen);
			}
		}
	}

	private static void tryBreedCluster(ServerLevel level, Animal seed, RandomSource random, Set<Integer> seen) {
		AABB area = seed.getBoundingBox().inflate(CLUSTER_RANGE);
		List<Animal> herd = level.getEntitiesOfClass(Animal.class, area, other -> sameKind(seed, other));
		int babies = 0;
		int adults = 0;
		List<Animal> ready = new ArrayList<>();
		for (Animal member : herd) {
			seen.add(member.getId());
			if (member.isBaby()) {
				babies++;
			} else {
				adults++;
				if (member.getAge() == 0 && member.isAlive()) {
					ready.add(member);
				}
			}
		}
		int cap = Math.max(1, adults) * babyMultiplier(id(seed.getType()));
		if (babies >= cap || ready.size() < 2) {
			return;
		}
		Collections.shuffle(ready, new java.util.Random(random.nextLong()));
		int room = cap - babies;
		Set<Integer> used = new HashSet<>();
		for (int i = 0; i < ready.size() && room > 0; i++) {
			Animal first = ready.get(i);
			if (!used.add(first.getId())) {
				continue;
			}
			Animal mate = nearestUnused(first, ready, used, PAIR_RANGE);
			if (mate == null) {
				used.remove(first.getId());
				continue;
			}
			used.add(mate.getId());
			breedVanilla(level, first, mate);
			room--;
		}
	}

	private static void breedVanilla(ServerLevel level, Animal a, Animal b) {
		a.setInLove(null);
		b.setInLove(null);
		a.spawnChildFromBreeding(level, b);
	}

	private static Animal nearestUnused(Animal origin, List<Animal> ready, Set<Integer> used, double range) {
		Animal best = null;
		double bestDist = range * range;
		for (Animal other : ready) {
			if (other == origin || used.contains(other.getId())) {
				continue;
			}
			double dist = origin.distanceToSqr(other);
			if (dist <= bestDist) {
				bestDist = dist;
				best = other;
			}
		}
		return best;
	}

	private static boolean supported(Animal animal) {
		String path = id(animal.getType());
		return path.equals("cow") || path.equals("pig") || path.equals("sheep") || path.equals("chicken");
	}

	private static boolean sameKind(Animal a, Animal b) {
		return a.getType() == b.getType();
	}

	private static int babyMultiplier(String path) {
		if (path.equals("chicken")) {
			return 3;
		}
		if (path.equals("pig")) {
			return 2;
		}
		return 1;
	}

	private static String id(EntityType<?> type) {
		Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
		return key == null ? "" : key.getPath();
	}
}
