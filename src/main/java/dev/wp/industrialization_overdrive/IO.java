package dev.wp.industrialization_overdrive;

import dev.wp.industrialization_overdrive.compat.AE2Integration;
import dev.wp.industrialization_overdrive.machines.blockentities.multiblock.PyrolyseOvenBlockEntity;
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
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.swedz.tesseract.neoforge.capabilities.CapabilitiesListeners;
import net.swedz.tesseract.neoforge.compat.mi.TesseractMI;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IO.ID)
public final class IO {
    public static final String ID = "industrialization_overdrive";
    public static final String NAME = "Industrialization Overdrive";

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(ID, name);
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public IO(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, IOConfig.SPEC);
        IOConfig.loadConfig();
        bus.addListener(FMLCommonSetupEvent.class, (event) -> IOConfig.loadConfig());

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

        bus.addListener(RegisterDataMapTypesEvent.class, IODataMaps::init);

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, DataMapsUpdatedEvent.class, (event) -> {
            event.ifRegistry(Registries.BLOCK, (registry) -> PyrolyseOvenBlockEntity.initTiers());
        });
    }
}
