package sonar.fluxnetworks.client.gui.basic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.world.entity.player.Player;
import sonar.fluxnetworks.common.connection.FluxMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GuiPopupHost extends GuiFocusable {

    private GuiPopupCore<?> mCurrentPopup;

    protected GuiPopupHost(@Nonnull FluxMenu menu, @Nonnull Player player) {
        super(menu, player);
    }

    //// OPEN POP UP \\\\

    public final void openPopup(GuiPopupCore<?> popup) {
        if (popup == null || popup.mHost != this) {
            return;
        }
        closePopup();
        mCurrentPopup = popup;
        // CORREÇÃO: init() agora é chamado SEM parâmetros
        mCurrentPopup.init();
        onPopupOpen(mCurrentPopup);
    }

    protected void onPopupOpen(GuiPopupCore<?> popup) {
    }

    //// CLOSE POP UP \\\\

    public final void closePopup() {
        if (mCurrentPopup != null) {
            onPopupClose(mCurrentPopup);
            mCurrentPopup.onClose();
            mCurrentPopup = null;
        }
    }

    protected void onPopupClose(GuiPopupCore<?> popup) {
    }

    @Nullable
    public final GuiPopupCore<?> getCurrentPopup() {
        return mCurrentPopup;
    }

    //// mouse moved \\\\

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        if (mCurrentPopup != null) {
            mCurrentPopup.mouseMoved(mouseX, mouseY);
            return;
        }
        if (onMouseMoved(mouseX, mouseY)) {
            return;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    protected boolean onMouseMoved(double mouseX, double mouseY) {
        return false;
    }

    //// mouse click \\\\

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.mouseClicked(event, doubleClick);
        }

        boolean result = onMouseClicked(event, doubleClick);

        for (GuiEventListener child : children()) {
            if (child.mouseClicked(event, doubleClick)) {
                setFocused(child);
                if (event.button() == 0) {
                    setDragging(true);
                }
                return true;
            }
        }

        boolean focused = false;
        for (GuiEventListener child : children()) {
            if (child instanceof EditBox editBox && editBox.isFocused() &&
                    editBox.isMouseOver(event.x(), event.y())) {
                focused = true;
                break;
            }
        }
        if (!focused) {
            setFocused(null);
            return true;
        }
        return result;
    }

    protected boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    //// mouse release \\\\

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.mouseReleased(event);
        }

        if (event.button() == 0 && isDragging()) {
            setDragging(false);
            if (getFocused() != null) {
                return getFocused().mouseReleased(event);
            }
        }

        return onMouseReleased(event) || super.mouseReleased(event);
    }

    public boolean onMouseReleased(MouseButtonEvent event) {
        return false;
    }

    //// mouse dragged \\\\

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.mouseDragged(event, deltaX, deltaY);
        }

        if (getFocused() != null && isDragging() && event.button() == 0) {
            return getFocused().mouseDragged(event, deltaX, deltaY);
        }

        return onMouseDragged(event, deltaX, deltaY) || super.mouseDragged(event, deltaX, deltaY);
    }

    public boolean onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return false;
    }

    //// mouse scrolled \\\\

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return onMouseScrolled(mouseX, mouseY, scrollX, scrollY) ||
                super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    //// key pressed \\\\

    @Override
    public final boolean keyPressed(KeyEvent event) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.keyPressed(event);
        }
        return onKeyPressed(event) || super.keyPressed(event);
    }

    public boolean onKeyPressed(KeyEvent event) {
        return false;
    }

    //// key released \\\\

    @Override
    public final boolean keyReleased(KeyEvent event) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.keyReleased(event);
        }
        return onKeyReleased(event) || super.keyReleased(event);
    }

    public boolean onKeyReleased(KeyEvent event) {
        return false;
    }

    //// char typed \\\\

    @Override
    public final boolean charTyped(CharacterEvent event) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.charTyped(event);
        }
        return onCharTyped(event) || super.charTyped(event);
    }

    public boolean onCharTyped(CharacterEvent event) {
        return false;
    }

    //// preedit updated \\\\

    @Override
    public final boolean preeditUpdated(@Nullable PreeditEvent event) {
        if (mCurrentPopup != null) {
            return mCurrentPopup.preeditUpdated(event);
        }
        return onPreeditUpdated(event) || super.preeditUpdated(event);
    }

    public boolean onPreeditUpdated(@Nullable PreeditEvent event) {
        return false;
    }

    //// INIT \\\\

    @Override
    public void init() {
        super.init();
        // CORREÇÃO: Não precisa mais passar parâmetros para o popup
        if (mCurrentPopup != null) {
            mCurrentPopup.init();
        }
    }

    //// MÉTODOS DE RENDERIZAÇÃO CORRIGIDOS \\\\

    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
    }

    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
    }

    /**
     * Método principal de renderização - substitui o antigo render()
     */
    @Override
    public final void extractRenderState(@Nonnull GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        // Primeiro, desenha o fundo
        renderBackground(gr);
        drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);

        for (Renderable widget : renderables) {
            widget.extractRenderState(gr, mouseX, mouseY, deltaTicks);
        }

        // Depois, desenha o foreground
        drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);

        // Se houver um popup aberto, desenha ele por cima
        if (mCurrentPopup != null) {
            mCurrentPopup.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
            mCurrentPopup.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        }

        // Processa elementos deferidos (tooltips, etc)
        gr.extractDeferredElements(mouseX, mouseY, deltaTicks);
    }

    protected void renderBackground(GuiGraphicsExtractor gr) {
    }

    @Override
    protected void containerTick() {
        super.containerTick();

    }
}
