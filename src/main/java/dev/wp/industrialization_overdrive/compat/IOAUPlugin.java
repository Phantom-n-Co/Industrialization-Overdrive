package dev.wp.industrialization_overdrive.compat;

import aztech.modern_industrialization.compat.almostunified.MIRecipeUnifier;
import com.almostreliable.unified.api.plugin.AlmostUnifiedNeoPlugin;
import com.almostreliable.unified.api.plugin.AlmostUnifiedPlugin;
import com.almostreliable.unified.api.unification.recipe.RecipeUnifierRegistry;
import dev.wp.industrialization_overdrive.IO;
import net.minecraft.resources.ResourceLocation;

@AlmostUnifiedNeoPlugin
public final class IOAUPlugin implements AlmostUnifiedPlugin {
    @Override
    public ResourceLocation getPluginId() {
        return IO.id("almost_unified");
    }

    @Override
    public void registerRecipeUnifiers(RecipeUnifierRegistry reg) {
        reg.registerForModId(IO.ID, new MIRecipeUnifier());
    }
}
