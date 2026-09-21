package ua.volodreamer.naturereborn.tick;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

import java.util.ArrayList;
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
				tryBreedCluster(level, animal, random);
			}
		}
	}

	private static void tryBreedCluster(ServerLevel level, Animal seed, RandomSource random) {
		AABB area = seed.getBoundingBox().inflate(CLUSTER_RANGE);
		List<Animal> herd = level.getEntitiesOfClass(Animal.class, area, other -> sameKind(seed, other));
		int babies = 0;
		List<Animal> ready = new ArrayList<>();
		for (Animal member : herd) {
			if (member.isBaby()) {
				babies++;
			} else if (member.getAge() == 0 && member.isAlive()) {
				ready.add(member);
			}
		}
		int cap = babyCap(seed.getType());
		if (babies >= cap || ready.size() < 2) {
			return;
		}
		int births = Math.min(cap - babies, ready.size() >= 3 ? 2 : 1);
		Animal first = ready.get(random.nextInt(ready.size()));
		Animal mate = nearestReady(first, ready, PAIR_RANGE);
		if (mate == null) {
			return;
		}
		breedVanilla(level, first, mate);
		births--;
		if (births <= 0) {
			return;
		}
		Animal third = null;
		for (Animal candidate : ready) {
			if (candidate == first || candidate == mate) {
				continue;
			}
			if (candidate.distanceToSqr(first) <= PAIR_RANGE * PAIR_RANGE || candidate.distanceToSqr(mate) <= PAIR_RANGE * PAIR_RANGE) {
				third = candidate;
				break;
			}
		}
		if (third == null) {
			return;
		}
		third.setAge(0);
		first.setAge(0);
		breedVanilla(level, third, first);
	}

	private static void breedVanilla(ServerLevel level, Animal a, Animal b) {
		a.setInLove(null);
		b.setInLove(null);
		a.spawnChildFromBreeding(level, b);
	}

	private static Animal nearestReady(Animal origin, List<Animal> ready, double range) {
		Animal best = null;
		double bestDist = range * range;
		for (Animal other : ready) {
			if (other == origin) {
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
		EntityType<?> type = animal.getType();
		return type == EntityType.COW || type == EntityType.PIG || type == EntityType.SHEEP || type == EntityType.CHICKEN;
	}

	private static boolean sameKind(Animal a, Animal b) {
		return a.getType() == b.getType();
	}

	private static int babyCap(EntityType<?> type) {
		if (type == EntityType.CHICKEN) {
			return 6;
		}
		if (type == EntityType.PIG) {
			return 3;
		}
		return 2;
	}
}
