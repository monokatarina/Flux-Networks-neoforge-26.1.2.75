package sonar.fluxnetworks.data.loot;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import sonar.fluxnetworks.register.RegistryBlocks;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class FluxBlockLoot extends BlockLootSubProvider {

    public FluxBlockLoot(HolderLookup.Provider lookupProvider) {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
    }

    @Nonnull
    @Override
    public Iterable<Block> getKnownBlocks() {
        return List.of(
                RegistryBlocks.FLUX_BLOCK,
                RegistryBlocks.FLUX_PLUG,
                RegistryBlocks.FLUX_POINT,
                RegistryBlocks.FLUX_CONTROLLER,
                RegistryBlocks.BASIC_FLUX_STORAGE,
                RegistryBlocks.HERCULEAN_FLUX_STORAGE,
                RegistryBlocks.GARGANTUAN_FLUX_STORAGE
        );
    }

    @Override
    protected void generate() {
        dropSelf(RegistryBlocks.FLUX_BLOCK);
        dropSelf(RegistryBlocks.FLUX_PLUG);
        dropSelf(RegistryBlocks.FLUX_POINT);
        dropSelf(RegistryBlocks.FLUX_CONTROLLER);
        dropSelf(RegistryBlocks.BASIC_FLUX_STORAGE);
        dropSelf(RegistryBlocks.HERCULEAN_FLUX_STORAGE);
        dropSelf(RegistryBlocks.GARGANTUAN_FLUX_STORAGE);
    }
}
