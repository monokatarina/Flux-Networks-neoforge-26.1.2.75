package sonar.fluxnetworks.client.gui.basic;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.client.gui.EnumNavigationTab;
import sonar.fluxnetworks.client.gui.button.FluxEditBox;
import sonar.fluxnetworks.common.connection.FluxMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GuiFocusable extends AbstractContainerScreen<FluxMenu> {

    public static final int TEXTURE_SIZE = 512;
    public static final int GUI_TEXTURE_SIZE = 688;
    public static final int GUI_WIDTH = 172;
    public static final int GUI_HEIGHT = 172;

    public static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            FluxNetworks.MODID, "textures/gui/gui_background.png");
    public static final Identifier FRAME = Identifier.fromNamespaceAndPath(
            FluxNetworks.MODID, "textures/gui/gui_frame.png");
    public static final Identifier ICON = Identifier.fromNamespaceAndPath(
            FluxNetworks.MODID, "textures/gui/gui_icon.png");

    public GuiFocusable(FluxMenu menu, @Nonnull Player player) {
        super(menu, player.getInventory(), CommonComponents.EMPTY, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        super.setFocused(listener);
        for (GuiEventListener child : children()) {
            if (child != listener && child instanceof FluxEditBox editBox) {
                if (editBox.isFocused()) {
                    editBox.setFocused(false);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        InputConstants.Key key = InputConstants.getKey(event);
        int keyCode = event.key();

        if (getFocused() != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE ||
                    keyCode == GLFW.GLFW_KEY_ENTER ||
                    keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                setFocused(null);
                return true;
            }
            if (getMinecraft().options.keyInventory.isActiveAndMatches(key)) {
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || getMinecraft().options.keyInventory.isActiveAndMatches(key)) {
            if (this instanceof GuiPopupCore<?> core) {
                core.mHost.closePopup();
                return true;
            }
            if (this instanceof GuiTabCore core) {
                if (core.getNavigationTab() == EnumNavigationTab.TAB_HOME) {
                    onClose();
                } else {
                    core.switchTab(EnumNavigationTab.TAB_HOME, true);
                }
            }
            return true;
        }
        boolean result = super.keyPressed(event);
        return result || getFocused() != null;
    }

    // REMOVIDO O containerTick() - não é mais necessário

    protected void blitBackgroundOrFrame(@Nonnull GuiGraphicsExtractor gr) {
        blitFullTexture(gr, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);
    }

    public static void blitFullTexture(@Nonnull GuiGraphicsExtractor gr, @Nonnull Identifier texture,
                                       int x, int y, int width, int height) {
        gr.blit(texture, x, y, x + width, y + height, 0, 1, 0, 1);
    }

    public static void blitIcon(@Nonnull GuiGraphicsExtractor gr, int x, int y, int width, int height,
                                float u, float v, float sourceWidth, float sourceHeight) {
        gr.blit(ICON, x, y, x + width, y + height,
                u / TEXTURE_SIZE, (u + sourceWidth) / TEXTURE_SIZE,
                v / TEXTURE_SIZE, (v + sourceHeight) / TEXTURE_SIZE);
    }

    public static int opaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }
}
