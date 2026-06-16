package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiFocusable;

public class ColorButton extends GuiButtonCore {

    public int mColor;
    private boolean mSelected;

    public ColorButton(GuiFocusable screen, int x, int y, int color) {
        super(screen, x, y, 12, 12);
        mColor = color;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (mSelected) {
            drawColorButtonFrame(gr, x, y, width, height, 0xFFFFFFFF);
        }
        gr.fill(x, y, x + width, y + height, mColor | 0xAA000000);
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    private void drawColorButtonFrame(GuiGraphicsExtractor gr, int x, int y, int width, int height, int color) {
        // Desenha a borda ao redor do botão de cor quando selecionado
        gr.fill(x, y, x + width, y + 1, color); // borda superior
        gr.fill(x, y + height - 1, x + width, y + height, color); // borda inferior
        gr.fill(x, y + 1, x + 1, y + height - 1, color); // borda esquerda
        gr.fill(x + width - 1, y + 1, x + width, y + height - 1, color); // borda direita
    }
}