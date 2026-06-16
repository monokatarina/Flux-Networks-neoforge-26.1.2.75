package sonar.fluxnetworks.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FluxLootTableProvider extends LootTableProvider {

    public FluxLootTableProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, Collections.emptySet(), List.of(
                new SubProviderEntry(FluxBlockLoot::new, LootContextParamSets.BLOCK)), lookupProvider);
    }
}
