package sonar.fluxnetworks.common.device;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import sonar.fluxnetworks.FluxConfig;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.energy.IItemEnergyConnector;
import sonar.fluxnetworks.api.network.NetworkMember;
import sonar.fluxnetworks.api.network.WirelessType;
import sonar.fluxnetworks.common.capability.FluxPlayer;
import sonar.fluxnetworks.common.connection.TransferHandler;
import sonar.fluxnetworks.common.integration.CuriosIntegration;
import sonar.fluxnetworks.common.util.EnergyUtils;
import sonar.fluxnetworks.common.util.FluxUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;

public class FluxControllerHandler extends TransferHandler {

    private static final Predicate<ItemStack> NOT_EMPTY = s -> !s.isEmpty();

    private final Map<ServerPlayer, Iterable<WirelessHandler>> mPlayers = new HashMap<>();
    private int mTimer;

    private long mDesired;

    final TileFluxController mDevice;

    public FluxControllerHandler(TileFluxController fluxController) {
        super(FluxConfig.defaultLimit);
        mDevice = fluxController;
    }

    @Override
    public void onCycleStart() {
        if (mTimer == 0) {
            updatePlayers();
        }
        if ((mTimer & 0x3) == 2) {
            mDesired = chargeAllItems(getLimit(), true);
        }
    }

    @Override
    public void onCycleEnd() {
        mBuffer += mChange = -sendToConsumers(Math.min(mBuffer, getLimit()));
        mTimer = ++mTimer & 0x3f;
    }

    @Override
    public void addToBuffer(long energy) {
        mBuffer += energy;
    }

    @Override
    public long getRequest() {
        return Math.max(mDesired - mBuffer, 0);
    }

    @Override
    public void onNetworkChanged() {
        super.onNetworkChanged();
        mPlayers.clear();
    }

    @Override
    public void writeCustomTag(@Nonnull CompoundTag tag, byte type) {
        super.writeCustomTag(tag, type);
        tag.putLong(FluxConstants.BUFFER, mBuffer);
    }

    private long sendToConsumers(long energy) {
        if ((mTimer & 0x3) != 0) return 0;
        return chargeAllItems(energy, false);
    }

    private long chargeAllItems(long energy, boolean simulate) {
        long remaining = energy;
        for (var player : mPlayers.entrySet()) {
            if (!player.getKey().isAlive()) {
                continue;
            }
            for (WirelessHandler handler : player.getValue()) {
                remaining = handler.chargeItems(remaining, simulate);
                if (remaining <= 0) {
                    return energy;
                }
            }
        }
        return energy - remaining;
    }

    private void updatePlayers() {
        mPlayers.clear();

        PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
        for (NetworkMember p : mDevice.getNetwork().getAllMembers()) {
            ServerPlayer player = playerList.getPlayer(p.getPlayerUUID());
            if (player == null || !player.isAlive()) {
                continue;
            }

            // CORRIGIDO: FluxPlayer.get() em vez de FluxUtils.getFluxPlayer
            FluxPlayer fluxPlayer = FluxPlayer.get(player);
            if (fluxPlayer == null) {
                continue;
            }
            if (fluxPlayer.getWirelessNetwork() != mDevice.getNetworkID()) {
                continue;
            }
            int wirelessMode = fluxPlayer.getWirelessMode();
            if (!WirelessType.ENABLE_WIRELESS.isActivated(wirelessMode)) {
                continue;
            }
            if ((wirelessMode & ~(1 << WirelessType.ENABLE_WIRELESS.ordinal())) == 0) {
                continue;
            }
            final Inventory inventory = player.getInventory();
            final List<WirelessHandler> handlers = new ArrayList<>();

            if (WirelessType.MAIN_HAND.isActivated(wirelessMode)) {
                handlers.add(new WirelessHandler(() -> new Iterator<>() {
                    private boolean mHasNext = true;

                    @Override
                    public boolean hasNext() {
                        return mHasNext;
                    }

                    @Override
                    public ItemStack next() {
                        if (mHasNext) {
                            mHasNext = false;
                            // CORRIGIDO: getSelectedItem() em vez de getSelected()
                            return inventory.getSelectedItem();
                        }
                        throw new NoSuchElementException();
                    }
                }, NOT_EMPTY));
            }

            if (WirelessType.OFF_HAND.isActivated(wirelessMode)) {
                // CORRIGIDO: Usar getItem(40) para offhand
                handlers.add(new WirelessHandler(() -> List.of(inventory.getItem(40)).iterator(), NOT_EMPTY));
            }

            if (WirelessType.HOT_BAR.isActivated(wirelessMode)) {
                // CORRIGIDO: Usar getNonEquipmentItems() e limitar aos primeiros 9 slots (hotbar)
                handlers.add(new WirelessHandler(() -> {
                    List<ItemStack> hotbar = inventory.getNonEquipmentItems().subList(0, 9);
                    return hotbar.iterator();
                }, stack -> {
                    ItemStack heldItem = inventory.getSelectedItem();
                    return !stack.isEmpty() && (heldItem.isEmpty() || heldItem != stack);
                }));
            }

            if (WirelessType.ARMOR.isActivated(wirelessMode)) {
                // CORRIGIDO: Os slots de armadura são 36-39
                handlers.add(new WirelessHandler(() -> {
                    List<ItemStack> armor = new ArrayList<>();
                    for (int i = 36; i <= 39; i++) {
                        armor.add(inventory.getItem(i));
                    }
                    return armor.iterator();
                }, NOT_EMPTY));
            }

            if (WirelessType.CURIOS.isActivated(wirelessMode) && FluxNetworks.isCuriosLoaded()) {
                handlers.add(new WirelessHandler(CuriosIntegration.getFlatStacks(player), NOT_EMPTY));
            }

            if (!handlers.isEmpty()) {
                mPlayers.put(player, handlers);
            }
        }
    }

    private record WirelessHandler(
            Iterable<ItemStack> stacks,
            Predicate<ItemStack> validator) {

        private long chargeItems(long remaining, boolean simulate) {
            for (ItemStack stack : stacks) {
                IItemEnergyConnector connector;
                if (!validator.test(stack) || (connector = EnergyUtils.getConnector(stack)) == null) {
                    continue;
                }
                if (connector.canSendTo(stack)) {
                    remaining -= connector.sendTo(remaining, stack, simulate);
                    if (remaining <= 0) {
                        return 0;
                    }
                }
            }
            return remaining;
        }
    }
}