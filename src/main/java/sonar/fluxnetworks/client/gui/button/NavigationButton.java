package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sonar.fluxnetworks.client.gui.EnumNavigationTab;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiFocusable;

public class NavigationButton extends GuiButtonCore {

    private final EnumNavigationTab mTab;
    private boolean mSelected = false;

    public NavigationButton(GuiFocusable screen, int x, int y, EnumNavigationTab tab) {
        super(screen, x, y, 16, 16);
        mTab = tab;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = isMouseHovered(mouseX, mouseY);
        int uOffset = 32 * mTab.ordinal();
        GuiFocusable.blitIcon(gr, x, y, width, height, uOffset, mSelected ? 32 : 0, 32, 32);

        if (hovered && mClickable) {
            gr.centeredText(screen.getMinecraft().font, mTab.getTranslatedName(),
                    x + width / 2, y - 10, 0xFFFFFFFF);
        }
    }

    public EnumNavigationTab getTab() {
        return mTab;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }
}
