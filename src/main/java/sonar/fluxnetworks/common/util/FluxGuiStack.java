package sonar.fluxnetworks.common.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.register.RegistryBlocks;

public class FluxGuiStack {

    public static final ItemStack FLUX_PLUG;
    public static final ItemStack FLUX_POINT;
    public static final ItemStack FLUX_CONTROLLER;

    public static final ItemStack BASIC_STORAGE;
    public static final ItemStack HERCULEAN_STORAGE;
    public static final ItemStack GARGANTUAN_STORAGE;

    static {
        // Criar os ItemStacks
        ItemStack stack1 = new ItemStack(RegistryBlocks.FLUX_PLUG);
        ItemStack stack2 = new ItemStack(RegistryBlocks.FLUX_POINT);
        ItemStack stack3 = new ItemStack(RegistryBlocks.FLUX_CONTROLLER);
        ItemStack stack4 = new ItemStack(RegistryBlocks.BASIC_FLUX_STORAGE);
        ItemStack stack5 = new ItemStack(RegistryBlocks.HERCULEAN_FLUX_STORAGE);
        ItemStack stack6 = new ItemStack(RegistryBlocks.GARGANTUAN_FLUX_STORAGE);

        // Adicionar o componente CUSTOM_DATA com a flag FLUX_COLOR
        addFluxColorComponent(stack1);
        addFluxColorComponent(stack2);
        addFluxColorComponent(stack3);

        FLUX_PLUG = stack1;
        FLUX_POINT = stack2;
        FLUX_CONTROLLER = stack3;
        BASIC_STORAGE = stack4;
        HERCULEAN_STORAGE = stack5;
        GARGANTUAN_STORAGE = stack6;
    }

    private static void addFluxColorComponent(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(FluxConstants.FLUX_COLOR, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}