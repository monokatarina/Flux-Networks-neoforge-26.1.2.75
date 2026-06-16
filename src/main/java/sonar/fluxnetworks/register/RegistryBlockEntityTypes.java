package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.common.device.TileFluxController;
import sonar.fluxnetworks.common.device.TileFluxPlug;
import sonar.fluxnetworks.common.device.TileFluxPoint;
import sonar.fluxnetworks.common.device.TileFluxStorage;

import java.util.Set;

public class RegistryBlockEntityTypes {
    public static BlockEntityType<TileFluxPlug> FLUX_PLUG;
    public static BlockEntityType<TileFluxPoint> FLUX_POINT;
    public static BlockEntityType<TileFluxController> FLUX_CONTROLLER;
    public static BlockEntityType<TileFluxStorage.Basic> BASIC_FLUX_STORAGE;
    public static BlockEntityType<TileFluxStorage.Herculean> HERCULEAN_FLUX_STORAGE;
    public static BlockEntityType<TileFluxStorage.Gargantuan> GARGANTUAN_FLUX_STORAGE;

    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            FLUX_PLUG = new BlockEntityType<>(TileFluxPlug::new, Set.of(RegistryBlocks.FLUX_PLUG));
            helper.register(RegistryBlocks.FLUX_PLUG_KEY, FLUX_PLUG);

            FLUX_POINT = new BlockEntityType<>(TileFluxPoint::new, Set.of(RegistryBlocks.FLUX_POINT));
            helper.register(RegistryBlocks.FLUX_POINT_KEY, FLUX_POINT);

            FLUX_CONTROLLER = new BlockEntityType<>(TileFluxController::new, Set.of(RegistryBlocks.FLUX_CONTROLLER));
            helper.register(RegistryBlocks.FLUX_CONTROLLER_KEY, FLUX_CONTROLLER);

            BASIC_FLUX_STORAGE = new BlockEntityType<>(TileFluxStorage.Basic::new, Set.of(RegistryBlocks.BASIC_FLUX_STORAGE));
            helper.register(RegistryBlocks.BASIC_FLUX_STORAGE_KEY, BASIC_FLUX_STORAGE);

            HERCULEAN_FLUX_STORAGE = new BlockEntityType<>(TileFluxStorage.Herculean::new, Set.of(RegistryBlocks.HERCULEAN_FLUX_STORAGE));
            helper.register(RegistryBlocks.HERCULEAN_FLUX_STORAGE_KEY, HERCULEAN_FLUX_STORAGE);

            GARGANTUAN_FLUX_STORAGE = new BlockEntityType<>(TileFluxStorage.Gargantuan::new, Set.of(RegistryBlocks.GARGANTUAN_FLUX_STORAGE));
            helper.register(RegistryBlocks.GARGANTUAN_FLUX_STORAGE_KEY, GARGANTUAN_FLUX_STORAGE);
        });
    }

    private RegistryBlockEntityTypes() {}
}