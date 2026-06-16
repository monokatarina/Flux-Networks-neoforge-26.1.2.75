package sonar.fluxnetworks.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class FluxBlockTagsProvider {

    public FluxBlockTagsProvider(PackOutput output,
                                 CompletableFuture<HolderLookup.Provider> lookupProvider,
                                 @Nullable Object existingFileHelper) {
    }
}
