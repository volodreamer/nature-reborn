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
import java.util.HashSet;
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
		if (!broken.is(BlockTags.LOGS)) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		if (tool.isEmpty() || !tool.is(ItemTags.AXES)) {
			return;
		}

		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(origin);
		seen.add(origin);
		int logs = 0;
		int leaves = 0;
		while (!queue.isEmpty() && logs < config.lumberjackMaxLogs) {
			BlockPos current = queue.removeFirst();
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (!seen.add(next) || current.distManhattan(origin) > 28) {
					continue;
				}
				BlockState state = server.getBlockState(next);
				if (state.is(BlockTags.LOGS)) {
					server.destroyBlock(next, true, player);
					logs++;
					queue.add(next);
				} else if (state.is(BlockTags.LEAVES) && leaves < config.lumberjackMaxLeaves) {
					server.destroyBlock(next, true, player);
					leaves++;
					queue.add(next);
				}
			}
		}
	}
}
