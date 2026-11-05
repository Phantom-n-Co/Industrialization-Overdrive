package dev.wp.industrialization_overdrive;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

// Lives outside the mixin package (Mixin forbids loading classes from it) and cannot use IOUtil: ModList is not ready at mixin time.
public final class IOMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "dev.wp.industrialization_overdrive.mixin.";

    // Mixins that only serve ME hatches and reference their classes.
    private static final Set<String> ME_HATCH_MIXINS = Set.of(
            "ConfigurableItemStackCapacityMixin",
            "ConfigurableScreenHandlerMEHatchMixin",
            "ConfigurableStackPlayerLockMixin",
            "CrafterComponentHelperUncappedOutputMixin",
            "CrafterComponentUncappedOutputMixin",
            "CrafterOutputLockCheckMixin",
            "MultipliedCrafterComponentRecipeLockMixin",
            "ShapeMatcherControllerLinkMixin",
            "client.MachineScreenMEHatchMixin"
    );

    private static final boolean AE2_PRESENT = LoadingModList.get().getModFileById("ae2") != null;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AE2_PRESENT || !mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return true;
        }
        return !ME_HATCH_MIXINS.contains(mixinClassName.substring(MIXIN_PACKAGE.length()));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
