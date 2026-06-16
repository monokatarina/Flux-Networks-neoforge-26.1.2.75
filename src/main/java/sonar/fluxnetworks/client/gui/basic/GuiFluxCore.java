package sonar.fluxnetworks.client.gui.basic;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager; // Mudança: ToastManager em vez de ToastComponent
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.api.energy.EnergyType;
import sonar.fluxnetworks.api.network.AccessLevel;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.item.ItemFluxConfigurator;
import sonar.fluxnetworks.common.integration.MUIIntegration;
import sonar.fluxnetworks.common.item.ItemAdminConfigurator;
import sonar.fluxnetworks.common.util.FluxUtils;
import sonar.fluxnetworks.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Gui that interacts flux networks.
 */
public abstract class GuiFluxCore extends GuiPopupHost {

    protected final List<GuiButtonCore> mButtons = new ArrayList<>();

    public final Player mPlayer; // client player

    private FluxNetwork mNetwork;

    public GuiFluxCore(@Nonnull FluxMenu menu, @Nonnull Player player) {
        super(menu, player);
        mPlayer = player;
        mNetwork = ClientCache.getNetwork(menu.mProvider.getNetworkID());
        menu.mOnResultListener = this::onResponse;
    }

    // this called from main thread
    private void onResponse(FluxMenu menu, int key, int code) {
        final FluxTranslate t = FluxTranslate.fromResponseCode(code);
        if (t != null) {
            if (FluxNetworks.isModernUILoaded()) {
                MUIIntegration.showToastError(t);
            } else {
                // CORREÇÃO: Usar ToastManager e SystemToast.SystemToastId
                ToastManager toastManager = getMinecraft().getToastManager();
                // Criar um SystemToastId personalizado ou usar um existente
                SystemToast.SystemToastId toastId = new SystemToast.SystemToastId(); // Usa o construtor padrão (5000ms)
                SystemToast toast = SystemToast.multiline(
                        getMinecraft(),
                        toastId,
                        Component.literal(FluxNetworks.NAME),
                        t.getComponent()
                );
                toastManager.addToast(toast);
            }
        }
        onResponseAction(key, code);
    }

    /**
     * @return the menu token
     */
    public int getToken() {
        return menu.containerId;
    }

    /**
     * @return current network
     */
    @Nonnull
    public FluxNetwork getNetwork() {
        return mNetwork;
    }

    protected void setCurrentNetwork(@Nonnull FluxNetwork network) {
        mNetwork = network;
    }

    /**
     * @return current access
     */
    @Nonnull
    public AccessLevel getAccessLevel() {
        return mNetwork.getPlayerAccess(mPlayer);
    }

    @Override
    public void init() {
        super.init();
        mButtons.clear();
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        for (GuiButtonCore button : mButtons) {
            button.drawButton(gr, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);

        // Background
        blitFullTexture(gr, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        int color = opaqueColor(mNetwork.getNetworkColor());
        // Frame com cor da rede
        blitFullTexture(gr, FRAME, leftPos, topPos, imageWidth, imageHeight);
    }

    // CORREÇÃO: onMouseClicked agora usa MouseButtonEvent
    @Override
    public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();

        for (GuiButtonCore button : mButtons) {
            if (button.isClickable() && button.isMouseHovered(mouseX, mouseY)) {
                onButtonClicked(button, (float) mouseX, (float) mouseY, mouseButton);
                return true;
            }
        }
        return super.onMouseClicked(event, doubleClick);
    }

    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mNetwork = ClientCache.getNetwork(menu.mProvider.getNetworkID());
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    /**
     * Render the network bar on the top.
     */
    protected void renderNetwork(GuiGraphicsExtractor gr, String name, int color, int y) {
        int x = leftPos + 20;
        int frameColor = opaqueColor(color);
        gr.fill(x - 1, y - 1, x + 136, y, frameColor);
        gr.fill(x - 1, y + 12, x + 136, y + 13, frameColor);
        gr.fill(x - 1, y, x, y + 12, frameColor);
        gr.fill(x + 135, y, x + 136, y + 12, frameColor);
        gr.fill(x, y, x + 135, y + 12, 0xEE111111);
        gr.fill(x, y, x + 3, y + 12, frameColor);
        gr.text(font, name, x + 4, y + 2, 0xFFFFFFFF);
    }

    /**
     * Render the energy change.
     */
    protected void renderTransfer(GuiGraphicsExtractor gr, IFluxDevice device, int x, int y) {
        gr.text(font, FluxUtils.getTransferInfo(device, EnergyType.FE), x, y, 0xFFFFFFFF);

        String text = device.getDeviceType().isStorage() ? FluxTranslate.ENERGY.get() : FluxTranslate.BUFFER.get();
        text += ": " + ChatFormatting.BLUE + EnergyType.FE.getStorage(device.getTransferBuffer());
        gr.text(font, text, x, y + 10, 0xFFFFFFFF);

        renderItemStack(gr, device.getDisplayStack(), x - 20, y + 1);
    }

    protected void renderItemStack(GuiGraphicsExtractor gr, ItemStack stack, int x, int y) {
        gr.item(stack, x, y);
        gr.itemDecorations(font, stack, x, y);
    }

    public void setConnectedNetwork(FluxNetwork network, String password) {
        if (menu.mProvider instanceof TileFluxDevice) {
            ClientMessages.tileNetwork(getToken(), (TileFluxDevice) menu.mProvider, network, password);
        } else if (menu.mProvider instanceof ItemFluxConfigurator.Provider) {
            ClientMessages.itemNetwork(getToken(), network, password);
        } else if (menu.mProvider instanceof ItemAdminConfigurator.Provider) {
            ClientCache.sAdminViewingNetwork = network.getNetworkID();
        }
    }

    /**
     * Called when a server response is received.
     *
     * @param key  the request key
     * @param code the response code
     */
    protected void onResponseAction(int key, int code) {
    }
}
