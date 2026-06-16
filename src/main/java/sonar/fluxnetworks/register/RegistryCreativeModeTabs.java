package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;

public class RegistryCreativeModeTabs {
    public static final Identifier CREATIVE_MODE_TAB_KEY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "tab");
    public static CreativeModeTab CREATIVE_MODE_TAB;

    public static void register(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            CREATIVE_MODE_TAB = CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + FluxNetworks.MODID))
                    .icon(() -> new ItemStack(RegistryItems.FLUX_CORE))
                    .displayItems((_, output) -> {
                        output.accept(RegistryItems.FLUX_BLOCK);
                        output.accept(RegistryItems.FLUX_PLUG);
                        output.accept(RegistryItems.FLUX_POINT);
                        output.accept(RegistryItems.FLUX_CONTROLLER);
                        output.accept(RegistryItems.BASIC_FLUX_STORAGE);
                        output.accept(RegistryItems.HERCULEAN_FLUX_STORAGE);
                        output.accept(RegistryItems.GARGANTUAN_FLUX_STORAGE);
                        output.accept(RegistryItems.FLUX_DUST);
                        output.accept(RegistryItems.FLUX_CORE);
                        output.accept(RegistryItems.FLUX_CONFIGURATOR);
                        output.accept(RegistryItems.ADMIN_CONFIGURATOR);
                    })
                    .build();
            helper.register(CREATIVE_MODE_TAB_KEY, CREATIVE_MODE_TAB);
        });
    }

    private RegistryCreativeModeTabs() {}
}