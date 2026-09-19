package ua.volodreamer.naturereborn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;
import ua.volodreamer.naturereborn.species.BiomeRates;
import ua.volodreamer.naturereborn.species.Species;
import ua.volodreamer.naturereborn.species.SpeciesRegistry;
import ua.volodreamer.naturereborn.tick.NatureTicker;
import ua.volodreamer.naturereborn.tick.TickStats;

public final class NatureRebornCommands {
	private NatureRebornCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("naturereborn")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("status").executes(NatureRebornCommands::status))
				.then(Commands.literal("scan").executes(NatureRebornCommands::scan))
				.then(Commands.literal("reload").executes(NatureRebornCommands::reload)));
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		NatureRebornConfig config = NatureReborn.config();
		ServerLevel level = source.getLevel();
		TickStats stats = TickStats.of(level);

		source.sendSuccess(() -> Component.literal("Nature Reborn " + config.versionTag()), false);
		source.sendSuccess(() -> Component.literal("master=" + config.masterEnabled
				+ " dimEnabled=" + NatureTicker.isDimensionEnabled(level, config)
				+ " species=" + SpeciesRegistry.size()
				+ " radius=" + config.playerFullRateDistanceChunks
				+ " rolls/chunk=" + config.natureRollsPerChunkTick), false);
		if (stats != null) {
			source.sendSuccess(() -> Component.literal(stats.summaryLine()), false);
		} else {
			source.sendSuccess(() -> Component.literal("No ticker samples yet in this dimension."), false);
		}
		return 1;
	}

	private static int scan(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BlockPos pos = BlockPos.containing(source.getPosition());
		Block block = level.getBlockState(pos).getBlock();
		Species species = SpeciesRegistry.match(block);
		if (species == null) {
			source.sendSuccess(() -> Component.literal("No species at " + pos.toShortString() + " (" + block + ")"), false);
			return 0;
		}
		BiomeRates rates = species.ratesFor(level, pos);
		source.sendSuccess(() -> Component.literal(species.id() + " kind=" + species.kind()
				+ " growth=" + rates.growth()
				+ " spread=" + rates.spread()
				+ " death=" + rates.death()), false);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		NatureReborn.setConfig(ua.volodreamer.naturereborn.config.NatureRebornConfig.load());
		context.getSource().sendSuccess(() -> Component.literal("Reloaded naturereborn.json. Use /reload for species datapacks."), false);
		return 1;
	}
}
