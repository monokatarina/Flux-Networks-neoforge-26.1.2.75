package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.integration.TOPIntegration;
import sonar.fluxnetworks.common.util.EnergyUtils;
import sonar.fluxnetworks.data.loot.FluxLootTableProvider;
import sonar.fluxnetworks.data.tags.FluxBlockTagsProvider;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = FluxNetworks.MODID)
public class Registration {

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        FMLChannel fmlChannel = new FMLChannel();
        Channel.sChannel = fmlChannel;

        event.enqueueWork(() -> {
            FluxNetworks.LOGGER.info("Chunk loading callback migration needed");
        });
        EnergyUtils.register();
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        FMLChannel.registerPayloads(event);
    }

    @SubscribeEvent
    public static void enqueueIMC(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("carryon")) {
            InterModComms.sendTo("carryon", "blacklistBlock", () -> FluxNetworks.MODID + ":*");
        }
        if (ModList.get().isLoaded("theoneprobe")) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", TOPIntegration::new);
        }
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(true, new FluxLootTableProvider(packOutput, event.getLookupProvider()));
        // Comentado temporariamente - precisa ajustar ExistingFileHelper
        // generator.addProvider(true, new FluxBlockTagsProvider(packOutput, event.getLookupProvider(), event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> RegistryBlocks.register(event));
        event.register(Registries.ITEM, helper -> RegistryItems.register(event));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> RegistryBlockEntityTypes.register(event));
        event.register(Registries.MENU, helper -> RegistryMenuTypes.register(event));
        event.register(Registries.RECIPE_SERIALIZER, helper -> RegistryRecipes.register(event));
        event.register(Registries.SOUND_EVENT, helper -> RegistrySounds.register(event));
        RegistryCreativeModeTabs.register(event);

        // REMOVIDO: FluxPlayer.ATTACHMENT_TYPES.register(event); - movido para o construtor do mod
    }
}
