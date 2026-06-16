package sonar.fluxnetworks.register;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkData;
import sonar.fluxnetworks.common.util.FluxCommands;

import java.util.List;

@EventBusSubscriber(modid = FluxNetworks.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FluxNetworkData.release();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        FluxNetworkData.getAllNetworks().forEach(FluxNetwork::onEndServerTick);
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.LeftClickBlock event) {
        if (!FluxConfig.enableFluxRecipe || event.getLevel().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState crusher = level.getBlockState(pos);
        BlockState base;
        if (crusher.getBlock() == Blocks.OBSIDIAN &&
                ((base = level.getBlockState(pos.below(2))).getBlock() == Blocks.BEDROCK ||
                        base.getBlock() == RegistryBlocks.FLUX_BLOCK)) {
            List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.below()));
            if (entities.isEmpty()) {
                return;
            }
            int itemCount = 0;
            for (ItemEntity entity : entities) {
                if (entity.getItem().is(Items.REDSTONE)) {
                    itemCount += entity.getItem().getCount();
                    entity.discard();
                    if (itemCount >= 512) {
                        break;
                    }
                }
            }
            if (itemCount == 0) {
                return;
            }
            ItemStack stack = new ItemStack(RegistryItems.FLUX_DUST, itemCount);
            level.removeBlock(pos, false);
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack);
            entity.setNoPickUpDelay();
            entity.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(entity);
            if (level.getRandom().nextDouble() > Math.pow(0.9, itemCount >> 3)) {
                level.setBlock(pos.below(), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
            } else {
                level.setBlock(pos.below(), crusher, Block.UPDATE_ALL);
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            int particleCount = Mth.clamp(itemCount >> 2, 4, 64);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY(),
                    pos.getZ() + 0.5, particleCount, 0, 0, 0, 0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        Channel.get().sendToPlayer(Messages.updateNetwork(
                FluxNetworkData.getAllNetworks(), FluxConstants.NBT_NET_BASIC), event.getEntity());
        Messages.syncCapability(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        FluxPlayer oldFluxPlayer = FluxPlayer.get(event.getOriginal());
        FluxPlayer newFluxPlayer = FluxPlayer.get(event.getEntity());
        newFluxPlayer.set(oldFluxPlayer);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FluxCommands.register(event.getDispatcher());
    }
}