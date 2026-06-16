package sonar.fluxnetworks.client.gui.tab;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.energy.EnergyType;
import sonar.fluxnetworks.client.gui.EnumNavigationTab;
import sonar.fluxnetworks.client.gui.basic.GuiTabCore;
import sonar.fluxnetworks.common.connection.*;
import sonar.fluxnetworks.common.util.FluxUtils;
import sonar.fluxnetworks.register.ClientMessages;

import javax.annotation.Nonnull;
import java.util.List;

public class GuiTabStatistics extends GuiTabCore {

    private LineChart mChart;
    private int timer = 0;

    public GuiTabStatistics(@Nonnull FluxMenu menu, @Nonnull Player player) {
        super(menu, player);
        if (getNetwork().isValid()) {
            ClientMessages.updateNetwork(getToken(), getNetwork(), FluxConstants.NBT_NET_STATISTICS);
        }
    }

    @Nonnull
    public EnumNavigationTab getNavigationTab() {
        return EnumNavigationTab.TAB_STATISTICS;
    }

    @Override
    protected void drawForegroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawForegroundLayer(gr, mouseX, mouseY, deltaTicks);
        final FluxNetwork network = getNetwork();
        if (network.isValid()) {
            int color = opaqueColor(network.getNetworkColor());
            renderNetwork(gr, network.getNetworkName(), color, topPos + 8);

            gr.pose().pushMatrix();
            gr.pose().translate(leftPos, topPos);
            final NetworkStatistics stats = network.getStatistics();
            gr.text(font, ChatFormatting.GRAY + FluxTranslate.PLUGS.get() + ChatFormatting.GRAY + ": " +
                    ChatFormatting.RESET + stats.fluxPlugCount, 12, 24, color);
            gr.text(font, ChatFormatting.GRAY + FluxTranslate.POINTS.get() + ChatFormatting.GRAY + ": " +
                    ChatFormatting.RESET + stats.fluxPointCount, 12, 36, color);
            gr.text(font, ChatFormatting.GRAY + FluxTranslate.STORAGES.get() + ChatFormatting.GRAY + ": " +
                    ChatFormatting.RESET + stats.fluxStorageCount, 82, 24, color);
            gr.text(font, ChatFormatting.GRAY + FluxTranslate.CONTROLLERS.get() + ChatFormatting.GRAY + ": " +
                    ChatFormatting.RESET + stats.fluxControllerCount, 82, 36, color);
            gr.text(font,
                    ChatFormatting.GRAY + FluxTranslate.INPUT.get() + ChatFormatting.GRAY + ": " + ChatFormatting.RESET +
                            EnergyType.FE.getUsage(stats.energyInput), 12, 48, color);
            gr.text(font,
                    ChatFormatting.GRAY + FluxTranslate.OUTPUT.get() + ChatFormatting.GRAY + ": " + ChatFormatting.RESET +
                            EnergyType.FE.getUsage(stats.energyOutput), 12, 60, color);
            gr.text(font,
                    ChatFormatting.GRAY + FluxTranslate.BUFFER.get() + ChatFormatting.GRAY + ": " + ChatFormatting.RESET +
                            EnergyType.FE.getStorage(stats.totalBuffer), 12, 72, color);
            gr.text(font,
                    ChatFormatting.GRAY + FluxTranslate.ENERGY.get() + ChatFormatting.GRAY + ": " + ChatFormatting.RESET +
                            EnergyType.FE.getStorage(stats.totalEnergy), 12, 84, color);
            gr.pose().scale(0.75f, 0.75f);
            gr.centeredText(font,
                    FluxTranslate.AVERAGE_TICK.get() + ": " + stats.averageTickMicro + " \u00b5s/t",
                    (int) ((imageWidth / 2f) * (1 / 0.75f)), (int) ((imageHeight - 2f) * (1 / 0.75f)), color);
            gr.pose().popMatrix();
        } else {
            renderNavigationPrompt(gr, FluxTranslate.ERROR_NO_SELECTED, EnumNavigationTab.TAB_SELECTION);
        }
    }

    @Override
    protected void drawBackgroundLayer(GuiGraphicsExtractor gr, int mouseX, int mouseY, float deltaTicks) {
        super.drawBackgroundLayer(gr, mouseX, mouseY, deltaTicks);
        if (getNetwork().isValid() && mChart != null) {
            mChart.drawChart(getMinecraft(), gr, deltaTicks);
        }
    }

    @Override
    public void init() {
        super.init();
        if (getNetwork().isValid()) {
            mChart = new LineChart(width / 2 - 48, height / 2 + 20, 50, NetworkStatistics.CHANGE_COUNT, "s",
                    EnergyType.FE.getStorageSuffix());
            mChart.updateData(getNetwork().getStatistics().energyChange);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int mouseButton = event.button();

        if (super.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (!getNetwork().isValid()) {
            return redirectNavigationPrompt(mouseX, mouseY, mouseButton, EnumNavigationTab.TAB_SELECTION);
        }
        return false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (getNetwork().isValid()) {
            timer = (timer + 1) % 20;
            if (timer == 0) {
                ClientMessages.updateNetwork(getToken(), getNetwork(), FluxConstants.NBT_NET_STATISTICS);
            }
        }
    }

    @Override
    protected void onResponseAction(int key, int code) {
        super.onResponseAction(key, code);
        if (code == FluxConstants.RESPONSE_REJECT) {
            switchTab(EnumNavigationTab.TAB_HOME, false);
            return;
        }
        if (key == FluxConstants.REQUEST_UPDATE_NETWORK) {
            if (mChart != null) {
                mChart.updateData(getNetwork().getStatistics().energyChange);
            }
        }
    }

    /**
     * Simple line chart - Versão simplificada que usa apenas fill()
     */
    public static class LineChart {

        private final int x, y;
        private final int height;
        private final int linePoints;
        private final String displayUnitX;
        private String displayUnitY;
        private long maxUnitY;
        private final String suffixUnitY;
        private LongList data = new LongArrayList();
        private final FloatList currentHeight;
        private final FloatList targetHeight;

        public LineChart(int x, int y, int height, int linePoints, String displayUnitX, String suffixUnitY) {
            this.x = x;
            this.y = y;
            this.height = height;
            this.linePoints = linePoints;
            this.displayUnitX = displayUnitX;
            this.suffixUnitY = suffixUnitY;

            this.currentHeight = new FloatArrayList(linePoints);
            for (int i = 0; i < linePoints; i++) {
                currentHeight.add(y + height);
            }
            this.targetHeight = new FloatArrayList(linePoints);
            for (int i = 0; i < linePoints; i++) {
                targetHeight.add(y + height);
            }
        }

        public void drawChart(Minecraft mc, GuiGraphicsExtractor gr, float deltaTicks) {
            // Desenhar linhas verticais
            for (int i = 0; i < currentHeight.size(); i++) {
                float cx = x + 20 * i;
                float cy = currentHeight.getFloat(i);
                // Pontos
                gr.fill((int) cx - 2, (int) cy - 2, (int) cx + 2, (int) cy + 2, 0xFFFFFFFF);
            }

            // Desenhar linhas conectando os pontos
            for (int i = 0; i < currentHeight.size() - 1; i++) {
                float x1 = x + 20 * i;
                float y1 = currentHeight.getFloat(i);
                float x2 = x + 20 * (i + 1);
                float y2 = currentHeight.getFloat(i + 1);
                drawLine(gr, (int) x1, (int) y1, (int) x2, (int) y2, 0xFFFFFFFF);
            }

            // Bordas do gráfico
            gr.fill(x - 16, y + height, x + 116, y + height + 1, 0xcfffffff);
            gr.fill(x - 14, y - 6, x - 13, y + height + 3, 0xcfffffff);

            // Textos
            gr.text(mc.font, suffixUnitY, x - 15 - mc.font.width(suffixUnitY), y - 7, 0xFFFFFFFF, true);
            if (displayUnitY != null) {
                gr.text(mc.font, displayUnitY, x - 15 - mc.font.width(displayUnitY), y, 0xFFFFFFFF, true);
            }
            gr.text(mc.font, displayUnitX, x + 118 - mc.font.width(displayUnitX), y + height + 2, 0xFFFFFFFF, true);

            for (int i = 0; i < data.size(); i++) {
                String d = FluxUtils.compact(data.getLong(i));
                gr.text(mc.font, d, (int) (x + 20 * i - mc.font.width(d) * 0.5f), (int) (currentHeight.getFloat(i) - 8), 0xFFFFFFFF, true);
                String c = String.valueOf((5 - i) * 5);
                gr.text(mc.font, c, (int) (x + 20 * i - mc.font.width(c) * 0.5f), y + height + 2, 0xFFFFFFFF, true);
            }

            updateHeight(deltaTicks);
        }

        private void drawLine(GuiGraphicsExtractor gr, int x1, int y1, int x2, int y2, int color) {
            // Algoritmo de linha de Bresenham simplificado
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int sx = x1 < x2 ? 1 : -1;
            int sy = y1 < y2 ? 1 : -1;
            int err = dx - dy;

            int x = x1, y = y1;
            while (true) {
                gr.fill(x, y, x + 1, y + 1, color);
                if (x == x2 && y == y2) break;
                int e2 = 2 * err;
                if (e2 > -dy) { err -= dy; x += sx; }
                if (e2 < dx) { err += dx; y += sy; }
            }
        }

        public void updateData(LongList newData) {
            this.data = newData;
            calculateUnitY(newData);
            calculateTargetHeight(newData);
        }

        private void updateHeight(float deltaTicks) {
            if (currentHeight.isEmpty()) {
                return;
            }
            for (int i = 0; i < currentHeight.size(); i++) {
                float diff = targetHeight.getFloat(i) - currentHeight.getFloat(i);
                if (diff == 0) continue;
                float p = deltaTicks / 16;
                float r = Math.abs(diff) <= p ? targetHeight.getFloat(i) :
                        currentHeight.getFloat(i) + (diff > 0 ? Math.max(Math.min(diff, diff / 4 * deltaTicks), p) :
                                Math.min(Math.max(diff, diff / 4 * deltaTicks), -p));
                currentHeight.set(i, r);
            }
        }

        private void calculateUnitY(@Nonnull List<Long> data) {
            long maxValue = data.stream().max(Long::compare).orElse(0L);
            if (maxValue <= 0) {
                displayUnitY = "1";
                maxUnitY = 1;
                return;
            }
            int exp = (int) Math.log10(maxValue);
            if (exp <= 0) {
                maxUnitY = maxValue + 1;
            } else if (exp <= 1) {
                maxUnitY = ((maxValue / 5) + 1) * 5;
            } else if (exp <= 2) {
                maxUnitY = ((maxValue / 50) + 1) * 50;
            } else {
                int unit = (int) Math.pow(10, exp);
                maxUnitY = ((maxValue / unit) + 1) * unit;
            }
            displayUnitY = FluxUtils.compact(maxUnitY);
            this.maxUnitY = maxUnitY;
        }

        private void calculateTargetHeight(@Nonnull List<Long> data) {
            if (data.size() != linePoints) return;
            for (int i = 0; i < data.size(); i++) {
                targetHeight.set(i, (float) (y + height * (1 - ((double) data.get(i) / maxUnitY))));
            }
        }
    }
}
