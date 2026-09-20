package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Lumberjack {
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
		if (player.isCreative() || isInstantBuild(player)) {
			return;
		}
		if (!broken.is(BlockTags.LOGS)) {
			return;
		}
		if (!server.isEmptyBlock(origin)) {
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

		for (BlockPos log : logs) {
			server.destroyBlock(log, true, player);
		}
		for (BlockPos leaf : leaves) {
			server.destroyBlock(leaf, true, player);
		}
	}

	private static boolean isInstantBuild(Player player) {
		try {
			return player.gameMode() == GameType.CREATIVE || player.gameMode() == GameType.SPECTATOR;
		} catch (Throwable ignored) {
			return player.isCreative();
		}
	}

	private static boolean isNaturalTree(ServerLevel level, BlockPos origin, List<BlockPos> logs, List<BlockPos> leaves) {
		if (leaves.size() < 8) {
			return false;
		}
		int tallest = 0;
		for (BlockPos log : logs) {
			tallest = Math.max(tallest, ForestEcology.trunkColumnHeight(level, log));
		}
		tallest = Math.max(tallest, ForestEcology.trunkColumnHeight(level, origin));
		return tallest >= 4 || leaves.size() >= 12;
	}

	private static void collect(ServerLevel level, BlockPos origin, List<BlockPos> logs, List<BlockPos> leaves, NatureRebornConfig config) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && logs.size() < config.lumberjackMaxLogs) {
			BlockPos current = queue.removeFirst();
			boolean fromLog = current.equals(origin) || logs.contains(current);
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (!seen.add(next) || current.distManhattan(origin) > 20) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.is(BlockTags.LOGS)) {
					if (!fromLog) {
						continue;
					}
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
}
