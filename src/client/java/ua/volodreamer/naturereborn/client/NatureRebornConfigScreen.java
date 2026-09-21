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
		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.general"));

		general.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.masterEnabled"), current.masterEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().masterEnabled = value)
				.build());
		general.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.overworldEnabled"), current.overworldEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().overworldEnabled = value)
				.build());
		general.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.netherEnabled"), current.netherEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().netherEnabled = value)
				.build());
		general.addEntry(entries.startDoubleField(Component.translatable("naturereborn.config.natureSpeed"), current.natureSpeed)
				.setDefaultValue(1.0)
				.setMin(0.0)
				.setMax(8.0)
				.setTooltip(Component.translatable("naturereborn.config.natureSpeed.tooltip"))
				.setSaveConsumer(value -> NatureReborn.config().natureSpeed = value)
				.build());

		ConfigCategory performance = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.performance"));
		performance.addEntry(entries.startIntField(Component.translatable("naturereborn.config.playerRadius"), current.playerFullRateDistanceChunks)
				.setDefaultValue(8)
				.setMin(0)
				.setMax(16)
				.setSaveConsumer(value -> NatureReborn.config().playerFullRateDistanceChunks = value)
				.build());
		performance.addEntry(entries.startIntField(Component.translatable("naturereborn.config.rollsPerChunk"), current.natureRollsPerChunkTick)
				.setDefaultValue(4)
				.setMin(0)
				.setMax(16)
				.setSaveConsumer(value -> NatureReborn.config().natureRollsPerChunkTick = value)
				.build());

		ConfigCategory plants = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.plants"));
		plants.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.grassEnabled"), current.grassEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().grassEnabled = value)
				.build());
		plants.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.plantsEnabled"), current.plantsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().plantsEnabled = value)
				.build());
		plants.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.weatheringEnabled"), current.weatheringEnabled)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("naturereborn.config.weatheringEnabled.tooltip"))
				.setSaveConsumer(value -> NatureReborn.config().weatheringEnabled = value)
				.build());

		ConfigCategory crops = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.crops"));
		crops.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.cropsEnabled"), current.cropsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropsEnabled = value)
				.build());
		crops.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.cropSpreadConvertsSoil"), current.cropSpreadConvertsSoil)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropSpreadConvertsSoil = value)
				.build());
		crops.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.cropSenescenceEnabled"), current.cropSenescenceEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().cropSenescenceEnabled = value)
				.build());
		crops.addEntry(entries.startDoubleField(Component.translatable("naturereborn.config.cropMatureLifeMultiplier"), current.cropMatureLifeMultiplier)
				.setDefaultValue(3.0)
				.setMin(1.0)
				.setMax(20.0)
				.setSaveConsumer(value -> NatureReborn.config().cropMatureLifeMultiplier = value)
				.build());

		ConfigCategory farm = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.farming"));
		farm.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.autoReplantEnabled"), current.autoReplantEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().autoReplantEnabled = value)
				.build());
		farm.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.autoReplantAllSources"), current.autoReplantAllSources)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().autoReplantAllSources = value)
				.build());
		farm.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.animalBreedingEnabled"), current.animalBreedingEnabled)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("naturereborn.config.animalBreedingEnabled.tooltip"))
				.setSaveConsumer(value -> NatureReborn.config().animalBreedingEnabled = value)
				.build());

		ConfigCategory trees = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.trees"));
		trees.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.treesEnabled"), current.treesEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().treesEnabled = value)
				.build());
		trees.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.fallenLogsEnabled"), current.fallenLogsEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().fallenLogsEnabled = value)
				.build());
		trees.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.lumberjackEnabled"), current.lumberjackEnabled)
				.setDefaultValue(false)
				.setTooltip(Component.translatable("naturereborn.config.lumberjackEnabled.tooltip"))
				.setSaveConsumer(value -> NatureReborn.config().lumberjackEnabled = value)
				.build());

		ConfigCategory fire = builder.getOrCreateCategory(Component.translatable("naturereborn.config.category.fire"));
		fire.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.fireEnabled"), current.fireEnabled)
				.setDefaultValue(true)
				.setSaveConsumer(value -> NatureReborn.config().fireEnabled = value)
				.build());
		fire.addEntry(entries.startBooleanToggle(Component.translatable("naturereborn.config.fireBurnUntilConsumed"), current.fireBurnUntilConsumed)
				.setDefaultValue(false)
				.setSaveConsumer(value -> NatureReborn.config().fireBurnUntilConsumed = value)
				.build());

		return builder.build();
	}
}
