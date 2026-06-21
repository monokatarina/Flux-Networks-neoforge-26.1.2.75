package sonar.fluxnetworks.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import sonar.fluxnetworks.common.device.TileFluxDevice;

public class FluxColorHandler implements BlockTintSource {

    public static final FluxColorHandler INSTANCE = new FluxColorHandler();

    @Override
    public int color(BlockState state) {
        return -1;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TileFluxDevice device) {
            return ARGB.opaque(device.mClientColor);
        }
        return -1;
    }
}
