package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiFocusable;

/**
 * A button with text and outer frame.
 */
public class SimpleButton extends GuiButtonCore {

    private final String mText;
    private int mColor;

    public SimpleButton(GuiFocusable screen, int x, int y, int width, int height, String text) {
        this(screen, x, y, width, height, text, 0xFFFFFFFF);
    }

    public SimpleButton(GuiFocusable screen, int x, int y, int width, int height, String text, int color) {
        super(screen, x, y, width, height);
        mText = text;
        mColor = color;
    }

    @Override
    protected void drawButton(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        int color;
        int r = mColor >> 16 & 0xFF;
        int g = mColor >> 8 & 0xFF;
        int b = mColor & 0xFF;

        if (mClickable) {
            if (isMouseHovered(mouseX, mouseY)) {
                color = mColor;
            } else {
                color = 0xFF000000 | (int) (r * 0.75) << 16 | (int) (g * 0.75) << 8 | (int) (b * 0.75);
            }
        } else {
            color = 0xFF000000 | (int) (r * 0.375) << 16 | (int) (g * 0.375) << 8 | (int) (b * 0.375);
        }

        // Nota: lightTexture().turnOnLightLayer() pode não ser necessário no novo sistema
        // Se precisar, mantenha, caso contrário, remova ou comente
        // screen.getMinecraft().gameRenderer.lightTexture().turnOnLightLayer();

        // outer stroke - desenha a borda
        drawSimpleButtonFrame(gr, x, y, width, height, color);

        // Usar centeredText em vez de drawCenteredString
        gr.centeredText(screen.getMinecraft().font, mText, x + width / 2, y + (height - 8) / 2, color);
    }

    public void setColor(int color) {
        mColor = color;
    }

    private void drawSimpleButtonFrame(GuiGraphicsExtractor gr, int x, int y, int width, int height, int color) {
        // Desenha a borda ao redor do botão
        gr.fill(x, y, x + width, y + 1, color); // borda superior
        gr.fill(x, y + height - 1, x + width, y + height, color); // borda inferior
        gr.fill(x, y + 1, x + 1, y + height - 1, color); // borda esquerda
        gr.fill(x + width - 1, y + 1, x + width, y + height - 1, color); // borda direita
    }
}