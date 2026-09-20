package ua.volodreamer.naturereborn.tick;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
		if (player.isCreative()) {
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
		collectLogs(server, origin, logs, config.lumberjackMaxLogs);
		List<BlockPos> leaves = collectLeaves(server, logs, origin, config.lumberjackMaxLeaves);
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

	private static boolean isNaturalTree(ServerLevel level, BlockPos origin, List<BlockPos> logs, List<BlockPos> leaves) {
		if (leaves.size() < 6 && logs.size() < 4) {
			return false;
		}
		int tallest = ForestEcology.trunkColumnHeight(level, origin);
		for (BlockPos log : logs) {
			tallest = Math.max(tallest, ForestEcology.trunkColumnHeight(level, log));
		}
		return leaves.size() >= 6 || tallest >= 4 || logs.size() >= 8;
	}

	private static void collectLogs(ServerLevel level, BlockPos origin, List<BlockPos> logs, int maxLogs) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && logs.size() < maxLogs) {
			BlockPos current = queue.removeFirst();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.offset(dx, dy, dz);
						if (!seen.add(next) || current.distManhattan(origin) > 28) {
							continue;
						}
						if (!level.getBlockState(next).is(BlockTags.LOGS)) {
							continue;
						}
						logs.add(next);
						queue.add(next);
					}
				}
			}
		}
	}

	private static List<BlockPos> collectLeaves(ServerLevel level, List<BlockPos> logs, BlockPos origin, int maxLeaves) {
		List<BlockPos> leaves = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		List<BlockPos> seeds = new ArrayList<>(logs);
		seeds.add(origin);
		for (BlockPos log : seeds) {
			for (int dx = -3; dx <= 3; dx++) {
				for (int dy = -2; dy <= 4; dy++) {
					for (int dz = -3; dz <= 3; dz++) {
						BlockPos pos = log.offset(dx, dy, dz);
						if (!seen.add(pos) || leaves.size() >= maxLeaves) {
							continue;
						}
						if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
							leaves.add(pos);
						}
					}
				}
			}
		}
		return leaves;
	}
}
