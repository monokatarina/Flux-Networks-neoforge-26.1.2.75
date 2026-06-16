package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.common.item.*;

public class RegistryItems {
    private static final Identifier FLUX_DUST_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_dust");
    private static final Identifier FLUX_CORE_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_core");
    private static final Identifier FLUX_CONFIGURATOR_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "flux_configurator");
    private static final Identifier ADMIN_CONFIGURATOR_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "admin_configurator");

    public static BlockItem FLUX_BLOCK;
    public static FluxDeviceItem FLUX_PLUG;
    public static FluxDeviceItem FLUX_POINT;
    public static FluxDeviceItem FLUX_CONTROLLER;
    public static FluxStorageItem BASIC_FLUX_STORAGE;
    public static FluxStorageItem HERCULEAN_FLUX_STORAGE;
    public static FluxStorageItem GARGANTUAN_FLUX_STORAGE;
    public static FluxDustItem FLUX_DUST;
    public static Item FLUX_CORE;
    public static ItemFluxConfigurator FLUX_CONFIGURATOR;
    public static ItemAdminConfigurator ADMIN_CONFIGURATOR;

    static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> {
            FLUX_BLOCK = new BlockItem(RegistryBlocks.FLUX_BLOCK, normalProps(RegistryBlocks.FLUX_BLOCK_KEY));
            helper.register(RegistryBlocks.FLUX_BLOCK_KEY, FLUX_BLOCK);

            FLUX_PLUG = new FluxDeviceItem(RegistryBlocks.FLUX_PLUG, normalProps(RegistryBlocks.FLUX_PLUG_KEY));
            helper.register(RegistryBlocks.FLUX_PLUG_KEY, FLUX_PLUG);

            FLUX_POINT = new FluxDeviceItem(RegistryBlocks.FLUX_POINT, normalProps(RegistryBlocks.FLUX_POINT_KEY));
            helper.register(RegistryBlocks.FLUX_POINT_KEY, FLUX_POINT);

            FLUX_CONTROLLER = new FluxDeviceItem(RegistryBlocks.FLUX_CONTROLLER, normalProps(RegistryBlocks.FLUX_CONTROLLER_KEY));
            helper.register(RegistryBlocks.FLUX_CONTROLLER_KEY, FLUX_CONTROLLER);

            BASIC_FLUX_STORAGE = new FluxStorageItem(RegistryBlocks.BASIC_FLUX_STORAGE, normalProps(RegistryBlocks.BASIC_FLUX_STORAGE_KEY));
            helper.register(RegistryBlocks.BASIC_FLUX_STORAGE_KEY, BASIC_FLUX_STORAGE);

            HERCULEAN_FLUX_STORAGE = new FluxStorageItem(RegistryBlocks.HERCULEAN_FLUX_STORAGE, normalProps(RegistryBlocks.HERCULEAN_FLUX_STORAGE_KEY));
            helper.register(RegistryBlocks.HERCULEAN_FLUX_STORAGE_KEY, HERCULEAN_FLUX_STORAGE);

            GARGANTUAN_FLUX_STORAGE = new FluxStorageItem(RegistryBlocks.GARGANTUAN_FLUX_STORAGE, normalProps(RegistryBlocks.GARGANTUAN_FLUX_STORAGE_KEY));
            helper.register(RegistryBlocks.GARGANTUAN_FLUX_STORAGE_KEY, GARGANTUAN_FLUX_STORAGE);

            FLUX_DUST = new FluxDustItem(normalProps(FLUX_DUST_KEY));
            helper.register(FLUX_DUST_KEY, FLUX_DUST);

            FLUX_CORE = new Item(normalProps(FLUX_CORE_KEY));
            helper.register(FLUX_CORE_KEY, FLUX_CORE);

            FLUX_CONFIGURATOR = new ItemFluxConfigurator(toolProps(FLUX_CONFIGURATOR_KEY));
            helper.register(FLUX_CONFIGURATOR_KEY, FLUX_CONFIGURATOR);

            ADMIN_CONFIGURATOR = new ItemAdminConfigurator(toolProps(ADMIN_CONFIGURATOR_KEY));
            helper.register(ADMIN_CONFIGURATOR_KEY, ADMIN_CONFIGURATOR);
        });
    }

    private static Item.Properties normalProps(Identifier id) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))
                .fireResistant();
    }

    private static Item.Properties toolProps(Identifier id) {
        return normalProps(id).stacksTo(1);
    }

    private RegistryItems() {}
}
