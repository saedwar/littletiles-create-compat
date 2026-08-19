package com.example.lcc;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.client.render.model.CreativeBakedBoxModel;
import team.creative.creativecore.common.util.math.base.Facing;

import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.math.box.LittleBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class LittleTilesMovementBehaviour implements MovementBehaviour {

    private static final WeakHashMap<BlockEntity, QuadCache> RENDER_CACHE =
            new WeakHashMap<>();

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderInContraption(
            MovementContext context,
            VirtualRenderWorld renderWorld,
            ContraptionMatrices matrices,
            MultiBufferSource buffer
    ) {
        BlockEntity be = renderWorld.getBlockEntity(context.localPos);

        if (!(be instanceof BETiles tilesBe)) {
            return;
        }

        QuadCache cache = RENDER_CACHE.computeIfAbsent(
                be,
                ignored -> buildQuadCache(tilesBe)
        );

        if (cache == null) {
            return;
        }

        PoseStack poseStack = matrices.getModelViewProjection();

        poseStack.pushPose();

        poseStack.translate(
                context.localPos.getX(),
                context.localPos.getY(),
                context.localPos.getZ()
        );

        PoseStack.Pose pose = poseStack.last();

        /*
         * For our first rendering test, use full brightness.
         * Once rendering works correctly, we can improve lighting.
         */
        int packedLight = getContraptionLight(
                context,
                renderWorld,
                matrices
        );        int overlay = OverlayTexture.NO_OVERLAY;

        /*
         * Render opaque / cutout geometry.
         */
        if (!cache.solidQuads.isEmpty()) {

            VertexConsumer solidConsumer = buffer.getBuffer(
                    RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)
            );

            for (ColoredQuad cq : cache.solidQuads) {

                solidConsumer.putBulkData(
                        pose,
                        cq.quad,
                        cq.r,
                        cq.g,
                        cq.b,
                        1.0f,
                        packedLight,
                        overlay,
                        true
                );
            }
        }

        /*
         * Render translucent geometry.
         */
        if (!cache.translucentQuads.isEmpty()) {

            VertexConsumer translucentConsumer = buffer.getBuffer(
                    RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)
            );

            for (ColoredQuad cq : cache.translucentQuads) {

                translucentConsumer.putBulkData(
                        pose,
                        cq.quad,
                        cq.r,
                        cq.g,
                        cq.b,
                        1.0f,
                        packedLight,
                        overlay,
                        true
                );
            }
        }

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private QuadCache buildQuadCache(BETiles be) {
        List<RenderBox> solidBoxes = getBoxesFromBE(be, false);
        List<RenderBox> transBoxes = getBoxesFromBE(be, true);

        QuadCache cache = new QuadCache();
        RandomSource rand = RandomSource.create();

        for (RenderBox box : solidBoxes) {
            for (Facing facing : Facing.VALUES) {
                box.setFace(facing, RenderBoxFace.RENDER);
            }
        }

        for (RenderBox box : transBoxes) {
            for (Facing facing : Facing.VALUES) {
                box.setFace(facing, RenderBoxFace.RENDER);
            }
        }

        // Build/cull solid quads
        for (int i = 0; i < solidBoxes.size(); i++) {
            RenderBox box1 = solidBoxes.get(i);
            List<RenderBox> singleList = Collections.singletonList(box1);

            for (Facing facing : Facing.VALUES) {
                List<BakedQuad> temp = new ArrayList<>();
                CreativeBakedBoxModel.compileBoxes(
                        singleList,
                        facing,
                        Sheets.cutoutBlockSheet(),
                        rand,
                        true,
                        temp
                );

                if (temp.isEmpty()) {
                    continue;
                }

                float[] b1PreciseBounds = getQuadBounds(temp);
                boolean culled = false;

                for (int j = 0; j < solidBoxes.size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    if (covers(b1PreciseBounds, solidBoxes.get(j), facing, 0, 0, 0)) {
                        culled = true;
                        break;
                    }
                }

                if (!culled && isTouchingBoundary(b1PreciseBounds, facing) && be.hasLevel()) {
                    BlockEntity neighborBE =
                            be.getLevel().getBlockEntity(
                                    be.getBlockPos().relative(facing.toVanilla())
                            );

                    if (neighborBE instanceof BETiles neighborTiles) {
                        List<RenderBox> neighborSolid = getBoxesFromBE(neighborTiles, false);

                        int ox = facing.toVanilla().getStepX();
                        int oy = facing.toVanilla().getStepY();
                        int oz = facing.toVanilla().getStepZ();

                        for (RenderBox box2 : neighborSolid) {
                            if (covers(b1PreciseBounds, box2, facing, ox, oy, oz)) {
                                culled = true;
                                break;
                            }
                        }
                    }
                }

                if (!culled) {
                    for (BakedQuad q : temp) {
                        cache.solidQuads.add(new ColoredQuad(q, box1));
                    }
                }
            }
        }

        // Build/cull translucent quads
        for (int i = 0; i < transBoxes.size(); i++) {
            RenderBox box1 = transBoxes.get(i);
            List<RenderBox> singleList = Collections.singletonList(box1);

            for (Facing facing : Facing.VALUES) {
                List<BakedQuad> temp = new ArrayList<>();
                CreativeBakedBoxModel.compileBoxes(
                        singleList,
                        facing,
                        Sheets.translucentCullBlockSheet(),
                        rand,
                        true,
                        temp
                );

                if (temp.isEmpty()) {
                    continue;
                }

                float[] b1PreciseBounds = getQuadBounds(temp);
                boolean culled = false;

                for (int j = 0; j < transBoxes.size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    if (covers(b1PreciseBounds, transBoxes.get(j), facing, 0, 0, 0)) {
                        culled = true;
                        break;
                    }
                }

                if (!culled && isTouchingBoundary(b1PreciseBounds, facing) && be.hasLevel()) {
                    BlockEntity neighborBE =
                            be.getLevel().getBlockEntity(
                                    be.getBlockPos().relative(facing.toVanilla())
                            );

                    if (neighborBE instanceof BETiles neighborTiles) {
                        List<RenderBox> neighborTrans = getBoxesFromBE(neighborTiles, true);

                        int ox = facing.toVanilla().getStepX();
                        int oy = facing.toVanilla().getStepY();
                        int oz = facing.toVanilla().getStepZ();

                        for (RenderBox box2 : neighborTrans) {
                            if (covers(b1PreciseBounds, box2, facing, ox, oy, oz)) {
                                culled = true;
                                break;
                            }
                        }
                    }
                }

                if (!culled) {
                    for (BakedQuad q : temp) {
                        cache.translucentQuads.add(new ColoredQuad(q, box1));
                    }
                }
            }
        }

        System.out.println(
                "[LCC] Baked LittleTiles quads at "
                        + be.getBlockPos()
                        + ": solidBoxes=" + solidBoxes.size()
                        + ", translucentBoxes=" + transBoxes.size()
                        + ", solidQuads=" + cache.solidQuads.size()
                        + ", translucentQuads=" + cache.translucentQuads.size()
        );

        return cache;
    }

    @OnlyIn(Dist.CLIENT)
    private List<RenderBox> getBoxesFromBE(
            BETiles be,
            boolean targetTranslucent
    ) {
        List<RenderBox> boxes = new ArrayList<>();

        RenderType layer = targetTranslucent
                ? RenderType.translucent()
                : RenderType.cutout();

        for (IParentCollection parent : be.groups()) {
            for (LittleTile tile : parent) {
                if (tile.isTranslucent() != targetTranslucent) {
                    continue;
                }

                for (LittleBox box : tile) {
                    RenderBox renderBox = parent.getRenderingBox(tile, box, layer);

                    if (renderBox != null) {
                        boxes.add(renderBox);
                    }
                }
            }
        }

        return boxes;
    }

    @OnlyIn(Dist.CLIENT)
    private static float[] getQuadBounds(List<BakedQuad> quads) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int step = data.length / 4;

            for (int i = 0; i < 4; i++) {
                int offset = i * step;

                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);

                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                if (z < minZ) minZ = z;
                if (z > maxZ) maxZ = z;
            }
        }

        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean covers(
            float[] b1,
            RenderBox b2,
            Facing face,
            int ox,
            int oy,
            int oz
    ) {
        if (b1 == null) {
            return false;
        }

        float e = 0.001f;

        float b2minX = b2.minX + ox;
        float b2maxX = b2.maxX + ox;
        float b2minY = b2.minY + oy;
        float b2maxY = b2.maxY + oy;
        float b2minZ = b2.minZ + oz;
        float b2maxZ = b2.maxZ + oz;

        return switch (face) {
            case EAST ->
                    Math.abs(b1[3] - b2minX) < e
                            && b2minY <= b1[1] + e
                            && b2maxY >= b1[4] - e
                            && b2minZ <= b1[2] + e
                            && b2maxZ >= b1[5] - e;

            case WEST ->
                    Math.abs(b1[0] - b2maxX) < e
                            && b2minY <= b1[1] + e
                            && b2maxY >= b1[4] - e
                            && b2minZ <= b1[2] + e
                            && b2maxZ >= b1[5] - e;

            case UP ->
                    Math.abs(b1[4] - b2minY) < e
                            && b2minX <= b1[0] + e
                            && b2maxX >= b1[3] - e
                            && b2minZ <= b1[2] + e
                            && b2maxZ >= b1[5] - e;

            case DOWN ->
                    Math.abs(b1[1] - b2maxY) < e
                            && b2minX <= b1[0] + e
                            && b2maxX >= b1[3] - e
                            && b2minZ <= b1[2] + e
                            && b2maxZ >= b1[5] - e;

            case SOUTH ->
                    Math.abs(b1[5] - b2minZ) < e
                            && b2minX <= b1[0] + e
                            && b2maxX >= b1[3] - e
                            && b2minY <= b1[1] + e
                            && b2maxY >= b1[4] - e;

            case NORTH ->
                    Math.abs(b1[2] - b2maxZ) < e
                            && b2minX <= b1[0] + e
                            && b2maxX >= b1[3] - e
                            && b2minY <= b1[1] + e
                            && b2maxY >= b1[4] - e;
        };
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isTouchingBoundary(float[] b1, Facing face) {
        float e = 0.001f;

        return switch (face) {
            case EAST -> Math.abs(b1[3] - 1.0f) < e;
            case WEST -> Math.abs(b1[0] - 0.0f) < e;
            case UP -> Math.abs(b1[4] - 1.0f) < e;
            case DOWN -> Math.abs(b1[1] - 0.0f) < e;
            case SOUTH -> Math.abs(b1[5] - 1.0f) < e;
            case NORTH -> Math.abs(b1[2] - 0.0f) < e;
        };
    }

    @OnlyIn(Dist.CLIENT)
    private static int getContraptionLight(
            MovementContext context,
            VirtualRenderWorld renderWorld,
            ContraptionMatrices matrices
    ) {
        Vector4f lightVec = new Vector4f(
                context.localPos.getX() + 0.5f,
                context.localPos.getY() + 0.5f,
                context.localPos.getZ() + 0.5f,
                1.0f
        );

        lightVec.mul(matrices.getLight());

        BlockPos worldLightPos = BlockPos.containing(
                lightVec.x(),
                lightVec.y(),
                lightVec.z()
        );

        int realLevelLight = LevelRenderer.getLightColor(
                context.world,
                worldLightPos
        );

        renderWorld.setExternalLight(realLevelLight);

        try {
            return LevelRenderer.getLightColor(
                    renderWorld,
                    context.localPos
            );
        } finally {
            renderWorld.resetExternalLight();
        }
    }


    @OnlyIn(Dist.CLIENT)
    public static class ColoredQuad {
        public final BakedQuad quad;
        public final float r;
        public final float g;
        public final float b;

        public ColoredQuad(BakedQuad quad, RenderBox box) {
            this.quad = quad;

            int c = box.color;

            if ((c == -1 || c == 0xFFFFFFFF) && quad.isTinted() && box.state != null) {
                int tint = Minecraft.getInstance()
                        .getBlockColors()
                        .getColor(box.state, null, null, quad.getTintIndex());

                if (tint != -1) {
                    c = tint | 0xFF000000;
                }
            }

            this.r = ((c >> 16) & 0xFF) / 255.0f;
            this.g = ((c >> 8) & 0xFF) / 255.0f;
            this.b = (c & 0xFF) / 255.0f;
        }
    }

    public static class QuadCache {
        public final List<ColoredQuad> solidQuads = new ArrayList<>();
        public final List<ColoredQuad> translucentQuads = new ArrayList<>();
    }
}