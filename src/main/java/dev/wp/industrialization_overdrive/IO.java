package dev.wp.industrialization_overdrive;

import aztech.modern_industrialization.MIText;
import aztech.modern_industrialization.util.TextHelper;
import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.datagen.client.provider.LanguageDatagenProvider;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.PyrolyseOvenBlockEntity;
import dev.wp.industrialization_overdrive.network.IOPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.loading.DatagenModLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.swedz.tesseract.api.Assert;
import net.swedz.tesseract.config.ConfigManager;
import net.swedz.tesseract.neoforge.capabilities.CapabilitiesListeners;
import net.swedz.tesseract.neoforge.compat.mi.TesseractMI;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.EuCostTransformer;
import net.swedz.tesseract.neoforge.compat.mi.tooltip.MIParser;
import net.swedz.tesseract.neoforge.config.ModConfigFileAccess;
import net.swedz.tesseract.neoforge.lang.LangManager;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;
import net.swedz.tesseract.neoforge.tooltip.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static aztech.modern_industrialization.MITooltips.DEFAULT_STYLE;
import static aztech.modern_industrialization.MITooltips.HIGHLIGHT_STYLE;

@Mod(IO.ID)
public final class IO {
    public static final String ID = "industrialization_overdrive";
    public static final String NAME = "Industrialization Overdrive";

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(ID, name);
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public IO(IEventBus bus, ModContainer container) {
        setupConfig(bus, container);
        setupText();

        TesseractMI.init(ID);
        IOComponents.init(bus);
        IOItems.init(bus);
        IOBlocks.init(bus);
        IOOtherRegistries.init(bus);

        bus.addListener(FMLCommonSetupEvent.class, (event) -> {
            IOItems.values().forEach(ItemHolder::triggerRegistrationListener);
            IOBlocks.values().forEach(BlockHolder::triggerRegistrationListener);
            if (IOUtil.isAE2Loaded) AE2Integration.registerItems();
        });

        bus.addListener(RegisterCapabilitiesEvent.class, (event) -> CapabilitiesListeners.triggerAll(ID, event));
        bus.addListener(RegisterPayloadHandlersEvent.class, IOPackets::init);

        bus.addListener(RegisterDataMapTypesEvent.class, IODataMaps::init);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DataMapsUpdatedEvent.class, (event) -> {
            event.ifRegistry(Registries.BLOCK, (registry) -> PyrolyseOvenBlockEntity.initTiers());
        });
    }

    private static IOConfig CONFIG;

    public static IOConfig config() {
        Assert.notNull(CONFIG, "Config not initialized yet");
        return CONFIG;
    }

    public static void setupConfig(IEventBus bus, ModContainer container) {
        var file = new ModConfigFileAccess(container, ModConfig.Type.STARTUP);
        var instance = new ConfigManager(file).build(IOConfig.class).load();
        bus.addListener(FMLCommonSetupEvent.class, (event) -> instance.load(false));
        file.registerReloadListeners(bus, instance);
        CONFIG = instance.config();
    }

    private static IOText TEXT;

    public static IOText text() {
        Assert.notNull(TEXT, "Text not initialized yet");
        return TEXT;
    }

    private static void setupText() {
        var instance = new LangManager(ID)
                .builtinColorStyles()
                .style("tooltip", () -> DEFAULT_STYLE)
                .style("highlighted", () -> HIGHLIGHT_STYLE)
                .builtinParsers()
                .parser("percentage", float.class, () -> (value) -> Parser.FLOAT_PERCENTAGE.parse(value, 0))
                .parser("eu_per_tick", long.class, () -> (value) -> {
                    var amount = TextHelper.getAmountGeneric(value);
                    return MIText.EuT.text(amount.digit(), amount.unit());
                })
                .parser("eu", long.class, () -> (value) -> {
                    var amount = TextHelper.getAmountGeneric(value);
                    return MIText.Eu.text(amount.digit(), amount.unit());
                })
                .parser(EuCostTransformer.class, () -> MIParser.EU_COST_TRANSFORMER_PARSER)
                .parser("block_pos", BlockPos.class, () -> Parser.BLOCK_POS)
                .parser("keybind", String.class, () -> Parser.KEYBIND)
                .build(IOText.class)
                .load();
        if (DatagenModLoader.isRunningDataGen())
            LanguageDatagenProvider.include(instance);
        TEXT = instance.lang();
    }
}
