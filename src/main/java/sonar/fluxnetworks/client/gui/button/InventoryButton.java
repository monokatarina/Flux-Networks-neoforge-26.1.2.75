package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.network.WirelessType;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.tab.GuiTabWireless;

import javax.annotation.Nonnull;

public class InventoryButton extends GuiButtonCore {

    public static final Identifier INVENTORY = Identifier.fromNamespaceAndPath(FluxNetworks.MODID, "textures/gui/inventory_configuration.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    public WirelessType mType;
    private final int mU0;
    private final int mV0;
    public GuiTabWireless mHost;
    private final String mText;

    public InventoryButton(GuiTabWireless screen, int x, int y, int width, int height, @Nonnull WirelessType type,
                           int u0, int v0) {
        super(screen, x, y, width, height);
        mHost = screen;
        mType = type;
        mText = type.getTranslatedName();
        mU0 = u0;
        mV0 = v0;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        int color = mHost.getNetwork().getNetworkColor();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int argbColor = ARGB.color(255, r, g, b);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY,
                x,
                y,
                mU0,
                mV0 + 11 * (mType.isActivated(mHost.mWirelessMode) ? 1 : 0),
                width,
                height,
                width,
                height,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                argbColor
        );

        if (isMouseHovered(mouseX, mouseY)) {
            Font font = screen.getMinecraft().font;
            graphics.text(font, mText, x + (width - font.width(mText)) / 2 + 1, y - 9, 0xFFFFFFFF, true);
        }
    }
}