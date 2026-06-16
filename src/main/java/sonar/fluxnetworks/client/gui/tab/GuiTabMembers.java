package sonar.fluxnetworks.client.gui.tab;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.network.NetworkMember;
import sonar.fluxnetworks.client.gui.EnumNavigationTab;
import sonar.fluxnetworks.client.gui.basic.GuiTabPages;
import sonar.fluxnetworks.client.gui.popup.PopupMemberEdit;
import sonar.fluxnetworks.common.connection.FluxMenu;
import sonar.fluxnetworks.common.util.FluxUtils;
import sonar.fluxnetworks.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.*;

public class GuiTabMembers extends GuiTabPages<NetworkMember> {

    public NetworkMember mSelectedMember;

    public GuiTabMembers(@Nonnull FluxMenu menu, @Nonnull Player player) {
        super(menu, player);
        mGridHeight = 13;
        mGridPerPage = 9;
        mElementWidth = 146;
        mElementHeight = 12;
        if (getNetwork().isValid()) {
            ClientMessages.updateNetwork(getToken(), getNetwork(), FluxConstants.NBT_NET_MEMBERS);
        }
    }

    @Nonnull
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_MEMBER;
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (getNetwork().isValid()) {
            String access = getAccessLevel().getFormattedName();
            gr.text(font, access, leftPos + 158 - font.width(access), topPos + 24, 0xFFFFFFFF);
            String sortBy = FluxTranslate.SORT_BY.get() + ": " + ChatFormatting.AQUA + mSortType.getTranslatedName();
            gr.text(font, sortBy, leftPos + 19, topPos + 24, 0xFFFFFFFF);

            renderNetwork(gr, getNetwork().getNetworkName(), getNetwork().getNetworkColor(), topPos + 8);
        } else {
            renderNavigationPrompt(gr, FluxTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
        }
    }

    @Override
    public void init() {
        super.init();
        mGridStartX = leftPos + 15;
        mGridStartY = topPos + 36;
        refreshPages(getNetwork().getAllMembers());
    }

    @Override
    protected void onElementClicked(NetworkMember element, int mouseButton) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            mSelectedMember = element;
            openPopup(new PopupMemberEdit(this));
        }
    }

    @Override
    public void renderElement(GuiGraphicsExtractor gr, NetworkMember element, int x, int y) {
        int color = element.getAccessLevel().getColor();

        renderElementFrame(gr, x, y, mElementWidth, mElementHeight, color);

        if (element.getPlayerUUID().equals(mPlayer.getUUID())) {
            gr.fill(x - 2, y, x - 1, y + mElementHeight, 0xFFFFFFFF);
            gr.fill(x + mElementWidth + 1, y, x + mElementWidth + 2, y + mElementHeight, 0xFFFFFFFF);
        }

        gr.text(font, ChatFormatting.WHITE + element.getCachedName(), x + 4, y + 2, 0xFFFFFFFF);

        String access = element.getAccessLevel().getFormattedName();
        gr.text(font, access, x + mElementWidth - 4 - font.width(access), y + 2, 0xFFFFFFFF);
    }

    @Override
    public void renderElementTooltip(GuiGraphicsExtractor gr, NetworkMember element, int mouseX, int mouseY) {
        List<Component> components = new ArrayList<>();
        components.add(FluxTranslate.USERNAME.makeComponent().append(": " + ChatFormatting.AQUA + element.getCachedName()));
        components.add(FluxTranslate.ACCESS.makeComponent().append(": " + element.getAccessLevel().getFormattedName()));

        // CORREÇÃO: usar setComponentTooltipForNextFrame em vez de renderComponentTooltip
        gr.setComponentTooltipForNextFrame(font, components, mouseX, mouseY);
    }

    // CORREÇÃO: onMouseClicked agora usa MouseButtonEvent
    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();

        if (super.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= leftPos + 45 && mouseX < leftPos + 75 && mouseY >= topPos + 24 && mouseY < topPos + 32) {
                mSortType = FluxUtils.cycle(mSortType, SortType.values());
                sortGrids(mSortType);
                refreshCurrentPage();
                return true;
            }
            if (!getNetwork().isValid()) {
                return redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
            }
        }
        return false;
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == FluxConstants.RESPONSE_REJECT) {
            switchTab(EnumNavigationTab.TAB_HOME, false);
            return;
        }
        if (key == FluxConstants.REQUEST_UPDATE_NETWORK) {
            refreshPages(getNetwork().getAllMembers());
        }
    }

    @Override
    protected void sortGrids(SortType sortType) {
        switch (sortType) {
            case ID ->
                    mElements.sort(Comparator.comparing(NetworkMember::getAccessLevel).thenComparing(NetworkMember::getPlayerUUID));
            case NAME ->
                    mElements.sort(Comparator.comparing(NetworkMember::getAccessLevel).thenComparing(NetworkMember::getCachedName));
        }
    }
}
