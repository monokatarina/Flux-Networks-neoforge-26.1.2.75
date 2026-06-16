package sonar.fluxnetworks.common.integration;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import sonar.fluxnetworks.FluxNetworks;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

/**
 * A holder class that prevents class-loading when Curios not available.
 *
 * @see FluxNetworks#isCuriosLoaded()
 */
public class CuriosIntegration {

    // NOTE: the return value will be no longer valid when the player dies (isAlive() == false)
    @Nonnull
    public static Iterable<ItemStack> getFlatStacks(ServerPlayer player) {
        Optional<ICuriosItemHandler> curios = CuriosApi.getCuriosInventory(player);
        return curios.<Iterable<ItemStack>>map(handler -> () -> new FlatIterator(handler))
                .orElse(Collections.emptyList());
    }

    private static class FlatIterator implements Iterator<ItemStack> {

        private final Iterator<ICurioStacksHandler> mIterator;

        private IDynamicStackHandler mHandler;
        private int mIndex;

        FlatIterator(ICuriosItemHandler curios) {
            mIterator = curios.getCurios().values().iterator();
        }

        @Override
        public boolean hasNext() {
            forward();
            return mHandler != null && mIndex < mHandler.getSlots();
        }

        @Override
        public ItemStack next() {
            forward();
            return mHandler.getStackInSlot(mIndex++);
        }

        private void forward() {
            while ((mHandler == null || mIndex == mHandler.getSlots()) && mIterator.hasNext()) {
                mHandler = mIterator.next().getStacks();
                mIndex = 0;
            }
        }
    }
}
