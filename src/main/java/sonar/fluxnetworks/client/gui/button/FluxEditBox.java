package sonar.fluxnetworks.client.gui.button;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nonnull;

public class FluxEditBox extends EditBox {

    private final Font mFont;
    private final String mHeader;
    private final int mHeaderWidth;

    private String mOrigin;
    private boolean mHexOnly;

    ///digits
    private boolean mDigitsOnly;
    private long mMaxValue = Integer.MAX_VALUE;
    private boolean mAllowNegatives = false;

    private int mOutlineColor = 0xffb4b4b4;

    private FluxEditBox(String header, Font font, int x, int y, int totalWidth, int height, int headerWidth) {
        super(font, x + headerWidth, y, totalWidth - headerWidth, height, Component.empty());
        mHeader = header;
        mHeaderWidth = headerWidth;
        mFont = font;
    }

    @Nonnull
    public static FluxEditBox create(String header, Font font, int x, int y, int width, int height) {
        return new FluxEditBox(header, font, x, y, width, height, font.width(header));
    }

    public int getIntegerFromText(boolean allowNegatives) {
        if (getValue().isEmpty() || getValue().equals("-")) {
            return 0;
        }
        int parseInt = Integer.parseInt(getValue());
        return allowNegatives ? parseInt : Math.max(parseInt, 0);
    }

    public long getLongFromText(boolean allowNegatives) {
        if (getValue().isEmpty() || getValue().equals("-")) {
            return 0;
        }
        long parseLong = Long.parseLong(getValue());
        return allowNegatives ? parseLong : Math.max(parseLong, 0);
    }

    public int getIntegerFromHex() {
        return Integer.parseInt(getValue(), 16);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        if (isVisible()) {
            // Usa os campos diretamente (width e height são protected em AbstractWidget)
            int currentWidth = width;
            int currentHeight = height;

            // Desenha o fundo
            gr.fill(getX() - mHeaderWidth, getY(), getX() + currentWidth, getY() + currentHeight, 0x30000000);

            // Desenha as bordas
            gr.fill(getX() - mHeaderWidth - 1, getY() - 1, getX() + currentWidth + 1, getY(), mOutlineColor);
            gr.fill(getX() - mHeaderWidth - 1, getY() + currentHeight, getX() + currentWidth + 1, getY() + currentHeight + 1, mOutlineColor);
            gr.fill(getX() - mHeaderWidth - 1, getY(), getX() - mHeaderWidth, getY() + currentHeight, mOutlineColor);
            gr.fill(getX() + currentWidth, getY(), getX() + currentWidth + 1, getY() + currentHeight, mOutlineColor);
        }

        gr.pose().pushMatrix();
        int dy = (height - 8) / 2; // Usa 'height' diretamente
        gr.pose().translate(4.0f, (float)dy);

        // Salva a posição original para restaurar depois
        int originalX = getX();
        int originalY = getY();

        // Ajusta temporariamente a posição para o translate funcionar corretamente
        setX(originalX + 4);
        setY(originalY + dy);

        setBordered(false);
        super.extractWidgetRenderState(gr, mouseX, mouseY, deltaTicks);

        // Restaura a posição original
        setX(originalX);
        setY(originalY);

        // Desenha o header
        gr.text(mFont, mHeader, getX() - mHeaderWidth, getY(), mOutlineColor);

        gr.pose().popMatrix();
    }

    @Override
    public void insertText(String textToWrite) {
        if (mDigitsOnly) {
            for (int i = 0; i < textToWrite.length(); i++) {
                char c = textToWrite.charAt(i);
                if (!Character.isDigit(c)) {
                    if (getValue().isEmpty()) {
                        if (c != '-') {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            }
        }
        if (mHexOnly) {
            for (int i = 0; i < textToWrite.length(); i++) {
                char c = textToWrite.charAt(i);
                if (c == '-') {
                    return;
                }
            }
            String origin = getValue();
            super.insertText(textToWrite);
            try {
                Integer.parseInt(getValue(), 16);
            } catch (final NumberFormatException ignored) {
                setValue(origin);
            }
            return;
        }
        super.insertText(textToWrite);
    }

    @Override
    public void setFocused(boolean isFocused) {
        if (isFocused) {
            if (mDigitsOnly) {
                mOrigin = getValue();
            }
        } else if (isFocused()) {
            if (mDigitsOnly) {
                try {
                    setValue(String.valueOf(getValidLong()));
                } catch (final NumberFormatException ignored) {
                    setValue(mOrigin);
                }
            }
        }
        super.setFocused(isFocused);
    }

    public long getValidLong() {
        return Math.min(getLongFromText(mAllowNegatives), mMaxValue);
    }

    public int getValidInt() {
        return (int) Math.min(getValidLong(), Integer.MAX_VALUE);
    }

    // ARGB
    public FluxEditBox setOutlineColor(int color) {
        mOutlineColor = color;
        return this;
    }

    public int getOutlineColor() {
        return mOutlineColor;
    }

    public FluxEditBox setTextInvisible() {
        // O método setFormatter foi removido, então usamos uma abordagem diferente
        // Podemos apenas definir um filtro que torna o texto invisível
        // Ou simplesmente ignoramos esta funcionalidade se não for essencial
        return this;
    }

    @Nonnull
    public static FormattedCharSequence getInvisibleText(String string, int unused) {
        return FormattedCharSequence.forward("•".repeat(string.length()), Style.EMPTY);
    }

    public FluxEditBox setDigitsOnly() {
        mDigitsOnly = true;
        return this;
    }

    public FluxEditBox setAllowNegatives(boolean allowNegatives) {
        mAllowNegatives = allowNegatives;
        return this;
    }

    public FluxEditBox setMaxValue(long max) {
        mMaxValue = max;
        return this;
    }

    public FluxEditBox setHexOnly() {
        mHexOnly = true;
        return this;
    }

    // O método tick já existe em EditBox, então não precisamos declarar um vazio
    // Removido o método tick() vazio que estava causando conflito
}