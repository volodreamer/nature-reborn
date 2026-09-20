package ua.volodreamer.naturereborn.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ua.volodreamer.naturereborn.NatureReborn;
import ua.volodreamer.naturereborn.config.NatureRebornConfig;

public final class NatureRebornConfigScreen {
	private NatureRebornConfigScreen() {
	}

	public static Screen create(Screen parent) {
		NatureRebornConfig current = NatureReborn.config();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("naturereborn.config.title"))
				.setSavingRunnable(() -> NatureRebornConfig.save(NatureReborn.config()));

		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

		general.addEntry(entries.startBooleanToggle(Component.literal("Enable Nature Reborn"), current.masterEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().masterEnabled = value)
				.build());

		general.addEntry(entries.startBooleanToggle(Component.literal("Overworld"), current.overworldEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().overworldEnabled = value)
				.build());

		general.addEntry(entries.startBooleanToggle(Component.literal("Nether"), current.netherEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().netherEnabled = value)
				.build());

		general.addEntry(entries.startDoubleField(Component.literal("Nature speed"), current.natureSpeed)
				.setDefaultValue(1.0)
				.setMin(0.0)
				.setMax(8.0)
				.setTooltip(Component.literal("Global multiplier for growth, spread and death chances. 0 pauses mutations. 3–4 is useful for debugging."))
				.setSaveConsumer(value -> NatureReborn.config().natureSpeed = value)
				.build());

		ConfigCategory performance = builder.getOrCreateCategory(Component.literal("Performance"));
		performance.addEntry(entries.startIntField(Component.literal("Player radius (chunks)"), current.playerFullRateDistanceChunks)
				.setDefaultValue(8)
				.setMin(0)
				.setMax(16)
				.setSaveConsumer(value -> NatureReborn.config().playerFullRateDistanceChunks = value)
				.build());
		performance.addEntry(entries.startIntField(Component.literal("Rolls per chunk tick"), current.natureRollsPerChunkTick)
				.setDefaultValue(4)
				.setMin(0)
				.setMax(16)
				.setSaveConsumer(value -> NatureReborn.config().natureRollsPerChunkTick = value)
				.build());

		ConfigCategory plants = builder.getOrCreateCategory(Component.literal("Plants"));
		plants.addEntry(entries.startBooleanToggle(Component.literal("Grass systems"), current.grassEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().grassEnabled = value)
				.build());
		plants.addEntry(entries.startBooleanToggle(Component.literal("Other plants"), current.plantsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().plantsEnabled = value)
				.build());

		ConfigCategory crops = builder.getOrCreateCategory(Component.literal("Crops"));
		crops.addEntry(entries.startBooleanToggle(Component.literal("Crop systems"), current.cropsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropsEnabled = value)
				.build());
		crops.addEntry(entries.startBooleanToggle(Component.literal("Convert dirt/grass to farmland on spread"), current.cropSpreadConvertsSoil)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropSpreadConvertsSoil = value)
				.build());
		crops.addEntry(entries.startBooleanToggle(Component.literal("Senescence (mature crops die and reseed)"), current.cropSenescenceEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropSenescenceEnabled = value)
				.build());
		crops.addEntry(entries.startDoubleField(Component.literal("Mature life multiplier"), current.cropMatureLifeMultiplier)
				.setDefaultValue(3.0)
				.setMin(1.0)
				.setMax(20.0)
				.setSaveConsumer(value -> NatureReborn.config().cropMatureLifeMultiplier = value)
				.build());

		ConfigCategory farm = builder.getOrCreateCategory(Component.literal("Farming"));
		farm.addEntry(entries.startBooleanToggle(Component.literal("Auto-replant on harvest"), current.autoReplantEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().autoReplantEnabled = value)
				.build());
		farm.addEntry(entries.startBooleanToggle(Component.literal("Auto-replant from all break sources"), current.autoReplantAllSources)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().autoReplantAllSources = value)
				.build());

		ConfigCategory trees = builder.getOrCreateCategory(Component.literal("Trees"));
		trees.addEntry(entries.startBooleanToggle(Component.literal("Tree lifecycle"), current.treesEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().treesEnabled = value)
				.build());
		trees.addEntry(entries.startBooleanToggle(Component.literal("Rare fallen logs"), current.fallenLogsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().fallenLogsEnabled = value)
				.build());
		trees.addEntry(entries.startBooleanToggle(Component.literal("Lumberjack (experimental)"), current.lumberjackEnabled)
				.setDefaultValue(false)
				.setTooltip(Component.literal("After a finished Survival axe break on a real tree, drop the rest of that tree. Off by default."))
				.setSaveConsumer(value -> NatureReborn.config().lumberjackEnabled = value)
				.build());

		ConfigCategory fire = builder.getOrCreateCategory(Component.literal("Fire"));
		fire.addEntry(entries.startBooleanToggle(Component.literal("Extended fire"), current.fireEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().fireEnabled = value)
				.build());
		fire.addEntry(entries.startBooleanToggle(Component.literal("Burn until consumed"), current.fireBurnUntilConsumed)
				.setDefaultValue(false)
				.setSaveConsumer(value -> NatureReborn.config().fireBurnUntilConsumed = value)
				.build());

		return builder.build();
	}
}
