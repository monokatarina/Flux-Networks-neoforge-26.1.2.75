package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sonar.fluxnetworks.api.gui.EnumNetworkColor;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiFocusable;

/**
 * A simple switch button with sliding thumb and track.
 */
public class SwitchButton extends GuiButtonCore {

    private static final int WIDTH = 16;
    private static final int HEIGHT = 8;

    // switch on/off
    private boolean mChecked = false;

    // thumb offset, fraction (0..1)
    private float mOffset;

    private int mColor;

    // default check state skips the animation

    public SwitchButton(GuiFocusable screen, int x, int y, boolean checked) {
        this(screen, x, y, checked, EnumNetworkColor.BLUE.getRGB());
    }

    public SwitchButton(GuiFocusable screen, int x, int y, boolean checked, int color) {
        super(screen, x, y, WIDTH, HEIGHT);
        if (checked) {
            mChecked = true;
            mOffset = 1;
        }
        mColor = color;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        mOffset = mChecked ? 1 : 0;
        int trackColor = mClickable ? (mChecked ? (mColor | 0xFF000000) : 0xFF606060) : 0xFF404040;
        int borderColor = isMouseHovered(mouseX, mouseY) && mClickable ? 0xFFFFFFFF : 0xFFD0D0D0;
        int knobX = x + Math.round(mOffset * (width - height));

        gr.fill(x, y + 2, x + width, y + height - 2, trackColor);
        gr.fill(x, y + 1, x + width, y + 2, borderColor);
        gr.fill(x, y + height - 2, x + width, y + height - 1, borderColor);
        gr.fill(knobX, y, knobX + height, y + height, 0xFFFFFFFF);
        gr.fill(knobX + 1, y + 1, knobX + height - 1, y + height - 1, mChecked ? 0xFFE8FFE8 : 0xFFE0E0E0);
    }

    public void toggle() {
        setChecked(!isChecked());
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    public void setColor(int color) {
        mColor = color;
    }
}
