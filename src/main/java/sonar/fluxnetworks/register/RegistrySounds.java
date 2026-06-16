package sonar.fluxnetworks.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import sonar.fluxnetworks.FluxNetworks;

public class RegistrySounds {
    public static final Identifier BUTTON_CLICK_KEY = FluxNetworks.location("button");

    public static SoundEvent BUTTON_CLICK;

    static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            BUTTON_CLICK = SoundEvent.createVariableRangeEvent(BUTTON_CLICK_KEY);
            helper.register(BUTTON_CLICK_KEY, BUTTON_CLICK);
        });
    }

    private RegistrySounds() {}
}