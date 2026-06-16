package sonar.fluxnetworks.client.gui.tab;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.network.AccessLevel;
import sonar.fluxnetworks.api.network.SecurityLevel;
import sonar.fluxnetworks.client.ClientCache;
import sonar.fluxnetworks.client.gui.EnumNavigationTab;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiTabPages;
import sonar.fluxnetworks.client.gui.button.EditButton;
import sonar.fluxnetworks.client.gui.button.SimpleButton;
import sonar.fluxnetworks.client.gui.popup.PopupNetworkPassword;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.device.TileFluxDevice;
import sonar.fluxnetworks.common.item.ItemAdminConfigurator;
import sonar.fluxnetworks.common.item.ItemFluxConfigurator;
import sonar.fluxnetworks.common.util.FluxUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class GuiTabSelection extends GuiTabPages<FluxNetwork> {

    private static final Path SORT_PREFS = Path.of("config", "fluxnetworks-client-prefs.properties");
    private static final String SORT_KEY = "networkSelectionSort";

    private EditButton mDisconnect;
    private SimpleButton mSortButton;
    public FluxNetwork mSelectedNetwork;

    public GuiTabSelection(@Nonnull FluxMenu menu, @Nonnull Player player) {
        super(menu, player);
        mSortType = readSavedSortType();
        mGridHeight = 13;
        mGridPerPage = 9;
        mElementWidth = 146;
        mElementHeight = 12;
    }

    @Override
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_SELECTION;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (mElements.isEmpty()) {
            renderNavigationPrompt(gr, FluxTranslate.ERROR_NO_NETWORK, EnumNavigationTab.TAB_CREATE);
        } else {
            String total = FluxTranslate.TOTAL.get() + ": " + mElements.size();
            gr.text(font, total, leftPos + 158 - font.width(total), topPos + 24, 0xFFFFFFFF);
            gr.text(font, FluxTranslate.SORT_BY.get() + ":", leftPos + 19, topPos + 24, 0xFFFFFFFF);

            renderNetwork(gr, getNetwork().getNetworkName(), getNetwork().getNetworkColor(), topPos + 8);
        }
    }

    @Override
    public void init() {
        super.init();
        mGridStartX = leftPos + 15;
        mGridStartY = topPos + 36;

        refreshPages(ClientCache.getAllNetworks());

        if (!mElements.isEmpty()) {
            int sortButtonWidth = Math.max(30, font.width(mSortType.getTranslatedName()) + 10);
            mSortButton = new SimpleButton(this, leftPos + 76, topPos + 21, sortButtonWidth, 14,
                    mSortType.getTranslatedName(), getSortButtonColor());
            mButtons.add(mSortButton);

            mDisconnect = new EditButton(this, leftPos + 142, topPos + 10, 8, 8, 0, 0,
                    FluxTranslate.BATCH_DISCONNECT_BUTTON.get(), FluxTranslate.BATCH_DISCONNECT_BUTTON.get());
            mDisconnect.setClickable(getNetwork().isValid());
            mButtons.add(mDisconnect);
        }
    }

    @Override
    public void renderElement(GuiGraphicsExtractor gr, FluxNetwork element, int x, int y) {
        boolean selected = isSelectedNetwork(element);
        boolean locked = element.getSecurityLevel() != SecurityLevel.PUBLIC;

        if (locked) {
            gr.fill(x + 136, y + 3, x + 142, y + 9, 0xFFD0D0D0);
            gr.fill(x + 137, y + 1, x + 141, y + 4, 0xFFD0D0D0);
            gr.fill(x + 138, y + 2, x + 140, y + 3, 0x30000000);
        }

        if (selected) {
            gr.fill(x - 2, y, x - 1, y + mElementHeight, 0xFFFFFFFF);
            gr.fill(x + mElementWidth + 1, y, x + mElementWidth + 2, y + mElementHeight, 0xFFFFFFFF);
        }

        renderBarAndName(gr, element, x, y, selected);
    }

    protected void renderBarAndName(GuiGraphicsExtractor gr, FluxNetwork element, int x, int y, boolean selected) {
        renderElementFrame(gr, x, y, mElementWidth, mElementHeight,
                selected ? 0xFFFFFFFF : getNetwork().getNetworkColor());

        gr.text(font, element.getNetworkName(), x + 4, y + 2, selected ? 0xFFFFFFFF : 0xFF606060);
    }

    @Override
    public void renderElementTooltip(GuiGraphicsExtractor gr, FluxNetwork element, int mouseX, int mouseY) {
        gr.setComponentTooltipForNextFrame(font, getElementTooltips(element), mouseX, mouseY);
    }

    protected List<Component> getElementTooltips(@Nonnull FluxNetwork element) {
        List<Component> components = new ArrayList<>();
        components.add(Component.literal("ID: " + element.getNetworkID()));
        components.add(FluxTranslate.NETWORK_NAME.makeComponent().append(": " +
                ChatFormatting.AQUA + element.getNetworkName()));
        components.add(FluxTranslate.NETWORK_SECURITY.makeComponent().append(": " +
                ChatFormatting.GOLD + element.getSecurityLevel().getName()));
        AccessLevel access = element.getPlayerAccess(mPlayer);
        if (access != AccessLevel.BLOCKED) {
            components.add(FluxTranslate.ACCESS.makeComponent().append(": " + access.getFormattedName()));
        }
        if (ClientCache.sWirelessNetwork == element.getNetworkID()) {
            components.add(FluxTranslate.EFFECTIVE_WIRELESS_NETWORK.makeComponent()
                    .withStyle(ChatFormatting.YELLOW));
        }
        return components;
    }

    @Override
    protected void onElementClicked(FluxNetwork element, int mouseButton) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mSelectedNetwork = element;
            setConnectedNetwork(element, ClientCache.getRecentPassword(element.getNetworkID()));
            refreshCurrentPage();
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();

        if (super.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mElements.isEmpty()) {
                return redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_CREATE);
            }
        }
        return false;
    }

    @Override
    public void onButtonClicked(GuiButtonCore button, float mouseX, float mouseY, int mouseButton) {
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (button == mSortButton) {
                cycleSortType();
            } else if (button == mDisconnect) {
                mSelectedNetwork = FluxNetwork.INVALID;
                setConnectedNetwork(FluxNetwork.INVALID, "");
                refreshCurrentPage();
            }
        }
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == FluxConstants.RESPONSE_REJECT) {
            switchTab(EnumNavigationTab.TAB_HOME, false);
            return;
        }
        if (key == FluxConstants.REQUEST_TILE_NETWORK) {
            if (code == FluxConstants.RESPONSE_REQUIRE_PASSWORD) {
                openPopup(new PopupNetworkPassword(this));
            } else if (code == FluxConstants.RESPONSE_SUCCESS) {
                if (mSelectedNetwork != null) {
                    if (getCurrentPopup() instanceof PopupNetworkPassword p) {
                        ClientCache.updateRecentPassword(mSelectedNetwork.getNetworkID(), p.mPassword.getValue());
                    }
                    if (menu.mProvider instanceof TileFluxDevice device) {
                        setCurrentNetwork(mSelectedNetwork);
                        device.setClientNetwork(mSelectedNetwork);
                    } else if (menu.mProvider instanceof ItemFluxConfigurator.Provider p) {
                        setCurrentNetwork(mSelectedNetwork);
                        // Obter o CustomData atual ou criar um novo
                        CustomData customData = p.mStack.get(DataComponents.CUSTOM_DATA);
                        CompoundTag rootTag;

                        if (customData != null) {
                            rootTag = customData.copyTag();
                        } else {
                            rootTag = new CompoundTag();
                        }

                        // CORREÇÃO AQUI: getCompound retorna Optional, usar orElse
                        CompoundTag fluxDataTag = rootTag.getCompound(FluxConstants.TAG_FLUX_CONFIG)
                                .orElse(new CompoundTag());

                        // Se o tag estava vazio, precisamos adicioná-lo ao root
                        if (!rootTag.contains(FluxConstants.TAG_FLUX_CONFIG)) {
                            rootTag.put(FluxConstants.TAG_FLUX_CONFIG, fluxDataTag);
                        }

                        // Salvar o network ID
                        fluxDataTag.putInt(FluxConstants.NETWORK_ID, mSelectedNetwork.getNetworkID());

                        // Atualizar o item
                        p.mStack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
                    }
                }
                closePopup();
                mSelectedNetwork = null;
                refreshPages(ClientCache.getAllNetworks());
            }
        } else if (key == FluxConstants.REQUEST_UPDATE_NETWORK) {
            refreshPages(ClientCache.getAllNetworks());
        }
    }
    @Override
    protected void containerTick() {
        super.containerTick();
            if (mDisconnect != null) {
            mDisconnect.setClickable(getNetwork().isValid());
        }
        if (mSortButton != null) {
            mSortButton.setColor(getSortButtonColor());
        }
    }

    @Override
    protected void sortGrids(SortType sortType) {
        switch (sortType) {
            case ID -> mElements.sort(Comparator.comparing(FluxNetwork::getNetworkID));
            case NAME -> mElements.sort(Comparator.comparing(FluxNetwork::getNetworkName));
        }
    }

    @Nonnull
    private static SortType readSavedSortType() {
        Properties properties = new Properties();
        if (Files.isRegularFile(SORT_PREFS)) {
            try (InputStream in = Files.newInputStream(SORT_PREFS)) {
                properties.load(in);
            } catch (IOException ignored) {
                return SortType.ID;
            }
        }
        try {
            return SortType.valueOf(properties.getProperty(SORT_KEY, SortType.ID.name()));
        } catch (IllegalArgumentException e) {
            return SortType.ID;
        }
    }

    private static void saveSortType(@Nonnull SortType sortType) {
        Properties properties = new Properties();
        properties.setProperty(SORT_KEY, sortType.name());
        try {
            Files.createDirectories(SORT_PREFS.getParent());
            try (OutputStream out = Files.newOutputStream(SORT_PREFS)) {
                properties.store(out, "Flux Networks client preferences");
            }
        } catch (IOException ignored) {
        }
    }

    private void cycleSortType() {
        mSortType = FluxUtils.cycle(mSortType, SortType.values());
        saveSortType(mSortType);
        switchTab(EnumNavigationTab.TAB_SELECTION, false);
    }

    private boolean isSelectedNetwork(@Nonnull FluxNetwork element) {
        int selectedId = getVisibleSelectedNetworkId();
        return selectedId != FluxConstants.INVALID_NETWORK_ID && selectedId == element.getNetworkID();
    }

    private int getVisibleSelectedNetworkId() {
        if (mSelectedNetwork != null) {
            return mSelectedNetwork.getNetworkID();
        }
        if (menu.mProvider instanceof TileFluxDevice || menu.mProvider instanceof ItemAdminConfigurator.Provider) {
            return getNetwork().getNetworkID();
        }
        return FluxConstants.INVALID_NETWORK_ID;
    }

    private int getSortButtonColor() {
        int color = getNetwork().isValid() ? getNetwork().getNetworkColor() : FluxConstants.INVALID_NETWORK_COLOR;
        return opaqueColor(color);
    }
}
