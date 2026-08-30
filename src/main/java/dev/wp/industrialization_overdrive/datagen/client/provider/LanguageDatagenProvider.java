package dev.wp.industrialization_overdrive.datagen.client.provider;

import aztech.modern_industrialization.MI;
import com.google.common.collect.Sets;
import dev.wp.industrialization_overdrive.IO;
import dev.wp.industrialization_overdrive.IOItems;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.swedz.tesseract.neoforge.datagen.mi.MIDatagenHooks;
import net.swedz.tesseract.neoforge.lang.LangInstance;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

import java.util.Set;

public final class LanguageDatagenProvider extends LanguageProvider {
    private static final Set<LangInstance<?>> INSTANCES = Sets.newHashSet();

    public static void include(LangInstance<?> instance) {
        INSTANCES.add(instance);
    }

    public LanguageDatagenProvider(GatherDataEvent event) {
        super(event.getGenerator().getPackOutput(), IO.ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        for (var instance : INSTANCES) {
            instance.datagen(this);
        }

        for (ItemHolder item : IOItems.values()) {
            this.add(item.asItem(), item.identifier().englishName());
        }

        MIDatagenHooks.Client.withLanguageHook(this, IO.ID);

        this.add("itemGroup.%s.%s".formatted(IO.ID, IO.ID), IO.NAME);
        this.add("key.%s.terminal_mode_switch".formatted(IO.ID), "Switch Multiblock Builder mode");
        this.add("key.categories.%s".formatted(IO.ID), IO.NAME);

        this.add("pyro_tier.%s.%s.%s".formatted(IO.ID, MI.ID, "cupronickel_coil"), "Cupronickel");
        this.add("pyro_tier.%s.%s.%s".formatted(IO.ID, MI.ID, "kanthal_coil"), "Kanthal");
    }
}
