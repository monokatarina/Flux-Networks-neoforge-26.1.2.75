package sonar.fluxnetworks.client.gui.basic;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

public abstract class GuiButtonCore {

    public final GuiFocusable screen;

    public int x;
    public int y;
    public int width;
    public int height;

    protected boolean mClickable = true;

    protected GuiButtonCore(GuiFocusable screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected abstract void drawButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks);

    public boolean isClickable() {
        return mClickable;
    }

    public void setClickable(boolean clickable) {
        mClickable = clickable;
    }

    public final boolean isMouseHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void drawOuterFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        // CORRIGIDO: usar fill com RenderPipelines.GUI
        graphics.fill(RenderPipelines.GUI, x - 1, y - 1, x + width + 1, y, color);
        graphics.fill(RenderPipelines.GUI, x - 1, y + height, x + width + 1, y + height + 1, color);
        graphics.fill(RenderPipelines.GUI, x - 1, y, x, y + height, color);
        graphics.fill(RenderPipelines.GUI, x + width, y, x + width + 1, y + height, color);
    }
}