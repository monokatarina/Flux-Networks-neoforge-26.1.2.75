package sonar.fluxnetworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import sonar.fluxnetworks.common.device.TileFluxStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FluxStorageEntityRenderer implements BlockEntityRenderer<TileFluxStorage, FluxStorageEntityRenderer.StorageRenderState> {

    private static final FluxStorageEntityRenderer INSTANCE = new FluxStorageEntityRenderer();

    public static final BlockEntityRendererProvider<TileFluxStorage, StorageRenderState> PROVIDER = INSTANCE::onContextChanged;

    private static final float START = 2.0f / 16.0f;
    private static final float END = 14.0f / 16.0f;
    private static final float OFFSET = 1.0f / 16.0f;
    private static final float WIDTH = 12.0f / 16.0f;
    private static final float HEIGHT = 13.0f / 16.0f;
    private static final int ALPHA = 150;

    private FluxStorageEntityRenderer() {}

    @Nonnull
    private FluxStorageEntityRenderer onContextChanged(@Nonnull BlockEntityRendererProvider.Context context) {
        return this;
    }

    @Override
    @Nonnull
    public StorageRenderState createRenderState() {
        return new StorageRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull TileFluxStorage blockEntity, @Nonnull StorageRenderState state,
                                   float partialTicks, @Nonnull Vec3 cameraPosition,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.color = blockEntity.mClientColor;
        state.energy = blockEntity.getTransferBuffer();
        state.capacity = blockEntity.getMaxTransferLimit();
    }

    @Override
    public void submit(@Nonnull StorageRenderState state, @Nonnull PoseStack poseStack,
                       @Nonnull SubmitNodeCollector submitNodeCollector, @Nonnull CameraRenderState camera) {
        if (state.energy <= 0 || state.capacity <= 0) return;

        RenderType renderType = FluxStorageRenderType.getType();
        submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                (pose, consumer) -> render(pose, consumer, state.color, state.energy, state.capacity));
    }

    public static void render(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer consumer,
                              int color, long energy, long capacity) {
        float renderHeight = Math.min(HEIGHT * energy / capacity, HEIGHT);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        renderSide(poseStack, consumer, Direction.NORTH, START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(poseStack, consumer, Direction.SOUTH, START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(poseStack, consumer, Direction.EAST,  START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(poseStack, consumer, Direction.WEST,  START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        if (renderHeight < HEIGHT) {
            renderSide(poseStack, consumer, Direction.UP, OFFSET, START + renderHeight, OFFSET, END, -END, r, g, b);
        }
    }

    private static void render(@Nonnull PoseStack.Pose pose, @Nonnull VertexConsumer consumer,
                               int color, long energy, long capacity) {
        float renderHeight = Math.min(HEIGHT * energy / capacity, HEIGHT);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        renderSide(pose, consumer, Direction.NORTH, START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(pose, consumer, Direction.SOUTH, START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(pose, consumer, Direction.EAST,  START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        renderSide(pose, consumer, Direction.WEST,  START, OFFSET, END, WIDTH, renderHeight, r, g, b);
        if (renderHeight < HEIGHT) {
            renderSide(pose, consumer, Direction.UP, OFFSET, START + renderHeight, OFFSET, END, -END, r, g, b);
        }
    }

    private static void renderSide(@Nonnull PoseStack stack, @Nonnull VertexConsumer consumer, @Nonnull Direction dir,
                                   float x, float y, float z, float width, float height,
                                   int r, int g, int b) {
        stack.pushPose();
        stack.translate(0.5, 0.5, 0.5);
        stack.mulPose(dir.getRotation());
        stack.translate(-0.5, -0.5, -0.5);

        PoseStack.Pose pose = stack.last();
        int light = 15728880; // FULL_BRIGHT
        int fullColor = (ALPHA << 24) | (r << 16) | (g << 8) | b;

        // Face frontal
        addEnergyVertex(consumer, pose, x, y, z - height, fullColor, 0, 1, light);
        addEnergyVertex(consumer, pose, x + width, y, z - height, fullColor, 1, 1, light);
        addEnergyVertex(consumer, pose, x + width, y, z, fullColor, 1, 0, light);
        addEnergyVertex(consumer, pose, x, y, z, fullColor, 0, 0, light);

        stack.popPose();
    }

    private static void renderSide(@Nonnull PoseStack.Pose pose, @Nonnull VertexConsumer consumer, @Nonnull Direction dir,
                                   float x, float y, float z, float width, float height,
                                   int r, int g, int b) {
        PoseStack.Pose transformedPose = pose.copy();
        transformedPose.translate(0.5f, 0.5f, 0.5f);
        transformedPose.rotate(dir.getRotation());
        transformedPose.translate(-0.5f, -0.5f, -0.5f);

        int light = 15728880; // FULL_BRIGHT
        int fullColor = (ALPHA << 24) | (r << 16) | (g << 8) | b;

        addEnergyVertex(consumer, transformedPose, x, y, z - height, fullColor, 0, 1, light);
        addEnergyVertex(consumer, transformedPose, x + width, y, z - height, fullColor, 1, 1, light);
        addEnergyVertex(consumer, transformedPose, x + width, y, z, fullColor, 1, 0, light);
        addEnergyVertex(consumer, transformedPose, x, y, z, fullColor, 0, 0, light);
    }

    private static void addEnergyVertex(@Nonnull VertexConsumer consumer, @Nonnull PoseStack.Pose pose,
                                        float x, float y, float z, int color, float u, float v, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 0, -1);
    }

    // Classe de estado para armazenar os dados do block entity
    public static class StorageRenderState extends BlockEntityRenderState {
        public int color;
        public long energy;
        public long capacity;
    }
}
