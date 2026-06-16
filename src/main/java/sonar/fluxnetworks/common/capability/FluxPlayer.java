package sonar.fluxnetworks.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;
import net.minecraft.server.permissions.PermissionSet;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class FluxPlayer implements ValueIOSerializable {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FluxNetworks.MODID);

    public static final Supplier<AttachmentType<FluxPlayer>> ATTACHMENT =
            ATTACHMENT_TYPES.register("flux_player", () -> AttachmentType.serializable(FluxPlayer::new)
                    .copyOnDeath()
                    .build());

    private boolean mSuperAdmin;
    private int mWirelessMode;
    private int mWirelessNetwork;

    public FluxPlayer() {
        this.mSuperAdmin = false;
        this.mWirelessMode = 0;
        this.mWirelessNetwork = -1;
    }

    public static FluxPlayer get(Player player) {
        return player.getData(ATTACHMENT);
    }

    public boolean isSuperAdmin() {
        return mSuperAdmin;
    }

    public boolean setSuperAdmin(boolean superAdmin) {
        if (mSuperAdmin != superAdmin) {
            mSuperAdmin = superAdmin;
            return true;
        }
        return false;
    }

    public int getWirelessMode() {
        return mWirelessMode;
    }

    public void setWirelessMode(int wirelessMode) {
        mWirelessMode = wirelessMode;
    }

    public int getWirelessNetwork() {
        return mWirelessNetwork;
    }

    public void setWirelessNetwork(int wirelessNetwork) {
        mWirelessNetwork = wirelessNetwork;
    }

    public void set(FluxPlayer other) {
        mSuperAdmin = other.mSuperAdmin;
        mWirelessMode = other.mWirelessMode;
        mWirelessNetwork = other.mWirelessNetwork;
    }

    public void writeNBT(@Nonnull CompoundTag tag) {
        tag.putBoolean("superAdmin", mSuperAdmin);
        tag.putInt("wirelessMode", mWirelessMode);
        tag.putInt("wirelessNetwork", mWirelessNetwork);
    }

    public void readNBT(@Nonnull CompoundTag tag) {
        mSuperAdmin = tag.getBooleanOr("superAdmin", false);
        mWirelessMode = tag.getIntOr("wirelessMode", 0);
        mWirelessNetwork = tag.getIntOr("wirelessNetwork", -1);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putBoolean("superAdmin", mSuperAdmin);
        output.putInt("wirelessMode", mWirelessMode);
        output.putInt("wirelessNetwork", mWirelessNetwork);
    }

    @Override
    public void deserialize(ValueInput input) {
        mSuperAdmin = input.getBooleanOr("superAdmin", false);
        mWirelessMode = input.getIntOr("wirelessMode", 0);
        mWirelessNetwork = input.getIntOr("wirelessNetwork", -1);
    }

    public static boolean canActivateSuperAdmin(Player player) {
        // Em singleplayer, sempre ativa o Super Admin se a config permitir
        return FluxConfig.enableSuperAdmin;
    }
    public static boolean isPlayerSuperAdmin(@Nonnull Player player) {
        if (FluxConfig.enableSuperAdmin) {
            return get(player).isSuperAdmin();
        }
        return false;
    }
}
