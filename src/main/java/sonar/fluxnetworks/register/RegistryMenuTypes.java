package sonar.fluxnetworks.register;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.item.ItemAdminConfigurator;
import sonar.fluxnetworks.common.item.ItemFluxConfigurator;

/**
 * ContainerType has the function to create container on client side<br>
 * Register the create container function that will be opened on client side from the packet that from the server
 */
public class RegistryMenuTypes {
    public static final Identifier FLUX_MENU_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_menu");
    public static MenuType<FluxMenu> FLUX_MENU;

    static void register(RegisterEvent event) {
        event.register(Registries.MENU, helper -> {
            FLUX_MENU = IMenuTypeExtension.create((containerId, inventory, buffer) -> {
                // check if it's tile entity
                if (buffer.readBoolean()) {
                    BlockPos pos = buffer.readBlockPos();
                    if (inventory.player.level().getBlockEntity(pos) instanceof TileFluxDevice device) {
                        CompoundTag tag = buffer.readNbt();
                        if (tag != null) {
                            device.readCustomTag(tag, FluxConstants.NBT_TILE_UPDATE);
                        }
                        return new FluxMenu(containerId, inventory, device);
                    }
                } else {
                    ItemStack stack = inventory.player.getMainHandItem();
                    if (stack.getItem() == RegistryItems.FLUX_CONFIGURATOR) {
                        return new FluxMenu(containerId, inventory, new ItemFluxConfigurator.Provider(stack));
                    }
                }
                return new FluxMenu(containerId, inventory, new ItemAdminConfigurator.Provider());
            });
            helper.register(FLUX_MENU_KEY, FLUX_MENU);
        });
    }

    private RegistryMenuTypes() {}
}