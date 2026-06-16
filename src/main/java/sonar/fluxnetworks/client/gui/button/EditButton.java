package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiFocusable;

/**
 * Button may have two states, and have different icons and texts.
 */
public class EditButton extends GuiButtonCore {

    private final int mCheckU0;
    private final int mUncheckU0;
    private final String mCheckText;
    private final String mUncheckText;

    private boolean mChecked = false;

    public EditButton(GuiFocusable screen, int x, int y,
                      int checkU0, int uncheckU0, String checkText, String uncheckText) {
        this(screen, x, y, 10, 10, checkU0, uncheckU0, checkText, uncheckText);
    }

    public EditButton(GuiFocusable screen, int x, int y, int width, int height,
                      int checkU0, int uncheckU0, String checkText, String uncheckText) {
        super(screen, x, y, width, height);
        mCheckU0 = checkU0;
        mUncheckU0 = uncheckU0;
        mCheckText = checkText;
        mUncheckText = uncheckText;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        boolean hovered = isMouseHovered(mouseX, mouseY);
        int color = mClickable ? (hovered ? 0xFFFFFFFF : 0xFFD0D0D0) : 0xFF707070;
        drawIcon(gr, mChecked ? mCheckU0 : mUncheckU0, color);

        if (hovered && mClickable) {
            String text = mChecked ? mCheckText : mUncheckText;
            gr.centeredText(screen.getMinecraft().font, text,
                    x + width / 2, y - 9, 0xFFFFFFFF);
        }
    }

    public void toggle() {
        mChecked = !mChecked;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    private void drawIcon(GuiGraphicsExtractor gr, int icon, int color) {
        drawFrame(gr, color);
        if (icon == 0) {
            drawX(gr, color);
        } else if (icon == 64) {
            gr.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0x30000000);
            gr.fill(x + 3, y + 3, x + width - 3, y + height - 3, color);
        } else if (icon == 128) {
            drawX(gr, color);
        } else if (icon == 192) {
            gr.fill(x + 3, y + height - 3, x + width - 2, y + height - 2, color);
            gr.fill(x + 4, y + height - 4, x + width - 3, y + height - 3, color);
            gr.fill(x + width - 4, y + 2, x + width - 2, y + 4, color);
            gr.fill(x + width - 5, y + 3, x + width - 3, y + 5, color);
            gr.fill(x + width - 6, y + 4, x + width - 4, y + 6, color);
        } else {
            gr.fill(x + 3, y + 3, x + width - 3, y + height - 3, color);
        }
    }

    private void drawFrame(GuiGraphicsExtractor gr, int color) {
        gr.fill(x, y, x + width, y + 1, color);
        gr.fill(x, y + height - 1, x + width, y + height, color);
        gr.fill(x, y + 1, x + 1, y + height - 1, color);
        gr.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    private void drawX(GuiGraphicsExtractor gr, int color) {
        for (int i = 2; i < width - 2; i++) {
            gr.fill(x + i, y + i, x + i + 1, y + i + 1, color);
            gr.fill(x + width - i - 1, y + i, x + width - i, y + i + 1, color);
        }
    }
}
