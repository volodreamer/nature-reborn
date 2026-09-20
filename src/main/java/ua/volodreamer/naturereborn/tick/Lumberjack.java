package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Lumberjack {
	private static final long WINDOW_TICKS = 200;
	private static final Map<ChopKey, ChopState> CHOPS = new ConcurrentHashMap<>();

	private Lumberjack() {
	}

	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(Lumberjack::afterBreak);
	}

	private static void afterBreak(Level level, Player player, BlockPos origin, BlockState broken, BlockEntity blockEntity) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		NatureRebornConfig config = NatureReborn.config();
		if (!config.masterEnabled || !config.lumberjackEnabled) {
			return;
		}
		if (config.lumberjackSneakBypass && player.isShiftKeyDown()) {
			return;
		}
		if (!broken.is(BlockTags.LOGS)) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		if (tool.isEmpty() || !tool.is(ItemTags.AXES)) {
			return;
		}

		List<BlockPos> logs = new ArrayList<>();
		List<BlockPos> leaves = new ArrayList<>();
		collect(server, origin, logs, leaves, config);
		if (!isNaturalTree(server, origin, logs, leaves)) {
			return;
		}

		BlockPos root = rootOf(origin, logs);
		long now = server.getGameTime();
		prune(now);
		ChopKey key = new ChopKey(server.dimension(), root.asLong());
		ChopState state = CHOPS.compute(key, (k, prev) -> {
			if (prev == null || now - prev.lastTick > WINDOW_TICKS) {
				return new ChopState(1, now);
			}
			return new ChopState(prev.count + 1, now);
		});

		if (state.count < 2) {
			dropNearbyLeaves(server, origin, player, 2);
			return;
		}

		CHOPS.remove(key);
		for (BlockPos log : logs) {
			server.destroyBlock(log, true, player);
		}
		for (BlockPos leaf : leaves) {
			server.destroyBlock(leaf, true, player);
		}
	}

	private static void dropNearbyLeaves(ServerLevel level, BlockPos origin, Player player, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -1; dy <= 3; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
						level.destroyBlock(pos, true, player);
					}
				}
			}
		}
	}

	private static BlockPos rootOf(BlockPos origin, List<BlockPos> logs) {
		BlockPos lowest = origin;
		for (BlockPos log : logs) {
			if (log.getY() < lowest.getY() || (log.getY() == lowest.getY() && log.asLong() < lowest.asLong())) {
				lowest = log;
			}
		}
		return lowest;
	}

	private static boolean isNaturalTree(ServerLevel level, BlockPos origin, List<BlockPos> logs, List<BlockPos> leaves) {
		if (leaves.size() < 8) {
			return false;
		}
		int tallest = ForestEcology.trunkColumnHeight(level, origin);
		for (BlockPos log : logs) {
			tallest = Math.max(tallest, ForestEcology.trunkColumnHeight(level, log));
		}
		return tallest >= 4 || leaves.size() >= 12;
	}

	private static void collect(ServerLevel level, BlockPos origin, List<BlockPos> logs, List<BlockPos> leaves, NatureRebornConfig config) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && logs.size() < config.lumberjackMaxLogs) {
			BlockPos current = queue.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (!seen.add(next) || current.distManhattan(origin) > 24) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.is(BlockTags.LOGS)) {
					if (isDecorative(level, next)) {
						continue;
					}
					logs.add(next);
					queue.add(next);
				} else if (state.is(BlockTags.LEAVES) && leaves.size() < config.lumberjackMaxLeaves) {
					leaves.add(next);
					queue.add(next);
				}
			}
		}
	}

	private static boolean isDecorative(ServerLevel level, BlockPos pos) {
		if (ForestEcology.trunkColumnHeight(level, pos) >= 3) {
			return false;
		}
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(pos.relative(direction)).is(BlockTags.LEAVES)) {
				return false;
			}
		}
		return true;
	}

	private static void prune(long now) {
		Iterator<Map.Entry<ChopKey, ChopState>> it = CHOPS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<ChopKey, ChopState> entry = it.next();
			if (now - entry.getValue().lastTick > WINDOW_TICKS) {
				it.remove();
			}
		}
	}

	private record ChopKey(ResourceKey<Level> dimension, long root) {
	}

	private record ChopState(int count, long lastTick) {
	}
}
