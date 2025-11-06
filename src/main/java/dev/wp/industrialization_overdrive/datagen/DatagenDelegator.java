package dev.wp.industrialization_overdrive.datagen;

import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.datagen.client.DatagenDelegatorClient;
import dev.wp.industrialization_overdrive.datagen.server.DatagenDelegatorServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = IO.ID)
public final class DatagenDelegator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DatagenDelegatorClient.configure(event);
        DatagenDelegatorServer.configure(event);
    }
}
