package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.common.block.FluxControllerBlock;
import sonar.fluxnetworks.common.block.FluxPlugBlock;
import sonar.fluxnetworks.common.block.FluxPointBlock;
import sonar.fluxnetworks.common.block.FluxStorageBlock;

public class RegistryBlocks {
    public static final Identifier FLUX_BLOCK_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_block");
    public static final Identifier FLUX_PLUG_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_plug");
    public static final Identifier FLUX_POINT_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_point");
    public static final Identifier FLUX_CONTROLLER_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_controller");
    public static final Identifier BASIC_FLUX_STORAGE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "basic_flux_storage");
    public static final Identifier HERCULEAN_FLUX_STORAGE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "herculean_flux_storage");
    public static final Identifier GARGANTUAN_FLUX_STORAGE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "gargantuan_flux_storage");

    public static Block FLUX_BLOCK;
    public static FluxPlugBlock FLUX_PLUG;
    public static FluxPointBlock FLUX_POINT;
    public static FluxControllerBlock FLUX_CONTROLLER;
    public static FluxStorageBlock.Basic BASIC_FLUX_STORAGE;
    public static FluxStorageBlock.Herculean HERCULEAN_FLUX_STORAGE;
    public static FluxStorageBlock.Gargantuan GARGANTUAN_FLUX_STORAGE;

    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            FLUX_BLOCK = new Block(normalProps(FLUX_BLOCK_KEY));
            helper.register(FLUX_BLOCK_KEY, FLUX_BLOCK);

            FLUX_PLUG = new FluxPlugBlock(deviceProps(FLUX_PLUG_KEY));
            helper.register(FLUX_PLUG_KEY, FLUX_PLUG);

            FLUX_POINT = new FluxPointBlock(deviceProps(FLUX_POINT_KEY));
            helper.register(FLUX_POINT_KEY, FLUX_POINT);

            FLUX_CONTROLLER = new FluxControllerBlock(deviceProps(FLUX_CONTROLLER_KEY));
            helper.register(FLUX_CONTROLLER_KEY, FLUX_CONTROLLER);

            BASIC_FLUX_STORAGE = new FluxStorageBlock.Basic(deviceProps(BASIC_FLUX_STORAGE_KEY));
            helper.register(BASIC_FLUX_STORAGE_KEY, BASIC_FLUX_STORAGE);

            HERCULEAN_FLUX_STORAGE = new FluxStorageBlock.Herculean(deviceProps(HERCULEAN_FLUX_STORAGE_KEY));
            helper.register(HERCULEAN_FLUX_STORAGE_KEY, HERCULEAN_FLUX_STORAGE);

            GARGANTUAN_FLUX_STORAGE = new FluxStorageBlock.Gargantuan(deviceProps(GARGANTUAN_FLUX_STORAGE_KEY));
            helper.register(GARGANTUAN_FLUX_STORAGE_KEY, GARGANTUAN_FLUX_STORAGE);
        });
    }

    private static BlockBehaviour.Properties normalProps(Identifier id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.0F, 1000F);
    }

    private static BlockBehaviour.Properties deviceProps(Identifier id) {
        return normalProps(id).noOcclusion();
    }

    private RegistryBlocks() {}
}
