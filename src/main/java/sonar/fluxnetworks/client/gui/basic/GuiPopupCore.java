package sonar.fluxnetworks.client.gui.basic;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class GuiPopupCore<T extends GuiFluxCore> extends GuiFocusable {

    protected final List<GuiButtonCore> mButtons = new ArrayList<>();

    public final T mHost;

    protected float mAlpha = 0;

    public GuiPopupCore(@Nonnull T host) {
        super(host.getMenu(), host.mPlayer);
        mHost = host;
    }

    public void init() {
        super.init();
        leftPos = Math.max(4, Math.min(leftPos, width - imageWidth - 4));
        topPos = Math.max(4, Math.min(topPos, height - imageHeight - 4));
        mButtons.clear();
    }

    @Override
    public void onClose() {
        mButtons.clear();
    }

    /**
     * Método para desenhar a camada de fundo.
     */
    public void drawBackgroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        mAlpha = Math.min(1.0f, mAlpha + deltaTicks / 6);

        // dimmer
        int bgColor = (int) (mAlpha * 128) << 24;
        gr.fill(0, 0, width, height, bgColor);

        // Background
        blitFullTexture(gr, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        int color = opaqueColor(mHost.getNetwork().getNetworkColor());
        // Frame com cor da rede
        blitFullTexture(gr, FRAME, leftPos, topPos, imageWidth, imageHeight);

        // --- Renderização dos widgets do Minecraft usando extractRenderState ---
        for (Renderable widget : renderables) {
            widget.extractRenderState(gr, mouseX, mouseY, deltaTicks);
        }
    }

    /**
     * Método para desenhar a camada de foreground.
     */
    public void drawForegroundLayer(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        // CORREÇÃO AQUI: Usar drawButton em vez de extractRenderState
        for (GuiButtonCore button : mButtons) {
            button.drawButton(gr, mouseX, mouseY, deltaTicks);
        }
    }

    // --- MÉTODO MOUSE CLICKED CORRIGIDO ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Extrai as coordenadas do evento
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();

        boolean result = false;
        // Lógica para seus botões customizados
        for (GuiButtonCore button : mButtons) {
            if (button.isClickable() && button.isMouseHovered(mouseX, mouseY)) {
                onButtonClicked(button, (int) mouseX, (int) mouseY, mouseButton);
                result = true;
                break;
            }
        }

        // Processa os filhos (EditBox, etc) usando o NOVO método mouseClicked
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(event, doubleClick)) {
                setFocused(child);
                if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    setDragging(true);
                }
                return true;
            }
        }

        // Lógica de foco para EditBox (adaptada)
        boolean focused = false;
        for (GuiEventListener child : this.children()) {
            if (child instanceof EditBox editBox && editBox.isFocused() &&
                    editBox.isMouseOver(mouseX, mouseY)) {
                focused = true;
                break;
            }
        }
        if (!focused) {
            setFocused(null);
        }
        return result;
    }

    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton) {
    }
}
