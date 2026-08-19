package com.example.lcc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.model.CreativeBakedBoxModel;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.littletiles.common.block.entity.BETiles;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

public class LittleTilesMovementBehaviour implements MovementBehaviour {

    private static final WeakHashMap<BlockEntity, LRCache> RENDER_CACHE = new WeakHashMap<>();

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BlockEntity be = renderWorld.getBlockEntity(context.localPos);
        if (!(be instanceof BETiles)) return;

        LRCache cache = RENDER_CACHE.computeIfAbsent(be, k -> buildCache((BETiles) k));
        if (cache == null) return;

        PoseStack poseStack = matrices.getModelViewProjection();
        poseStack.pushPose();
        poseStack.translate(context.localPos.getX(), context.localPos.getY(), context.localPos.getZ());
        PoseStack.Pose pose = poseStack.last();

        int packedLight = 15728880;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        net.minecraft.resources.ResourceLocation blockAtlas = InventoryMenu.BLOCK_ATLAS;

        if (!cache.solidQuads.isEmpty()) {
            com.mojang.blaze3d.vertex.VertexConsumer solidConsumer = buffer.getBuffer(RenderType.entityCutout(blockAtlas));
            for (ColoredQuad cq : cache.solidQuads) {
                float shade = getDirectionalShade(cq.quad);
                solidConsumer.putBulkData(pose, cq.quad, cq.r * shade, cq.g * shade, cq.b * shade, 1.0f, packedLight, overlay, true);
            }
        }

        if (!cache.translucentQuads.isEmpty()) {
            com.mojang.blaze3d.vertex.VertexConsumer transConsumer = buffer.getBuffer(RenderType.entityTranslucentCull(blockAtlas));
            for (ColoredQuad cq : cache.translucentQuads) {
                float shade = getDirectionalShade(cq.quad);
                transConsumer.putBulkData(pose, cq.quad, cq.r * shade, cq.g * shade, cq.b * shade, 1.0f, packedLight, overlay, true);
            }
        }
        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private LRCache buildCache(BETiles be) {
        try {
            List<RenderBox> solidBoxes = getBoxesFromBE(be, false);
            List<RenderBox> transBoxes = getBoxesFromBE(be, true);

            LRCache cache = new LRCache();
            net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create();

            for (RenderBox box : solidBoxes) {
                for (Facing facing : Facing.VALUES) {
                    box.setFace(facing, team.creative.creativecore.client.render.face.RenderBoxFace.RENDER);
                }
            }
            for (RenderBox box : transBoxes) {
                for (Facing facing : Facing.VALUES) {
                    box.setFace(facing, team.creative.creativecore.client.render.face.RenderBoxFace.RENDER);
                }
            }

            for (int i = 0; i < solidBoxes.size(); i++) {
                RenderBox box1 = solidBoxes.get(i);
                List<RenderBox> singleList = Collections.singletonList(box1);

                for (Facing facing : Facing.VALUES) {
                    List<BakedQuad> temp = new ArrayList<>();
                    CreativeBakedBoxModel.compileBoxes(singleList, facing, net.minecraft.client.renderer.Sheets.cutoutBlockSheet(), rand, true, temp);
                    if (temp.isEmpty()) continue;

                    float[] b1PreciseBounds = getQuadBounds(temp);
                    boolean culled = false;

                    for (int j = 0; j < solidBoxes.size(); j++) {
                        if (i == j) continue;
                        if (covers(b1PreciseBounds, solidBoxes.get(j), facing, 0, 0, 0)) {
                            culled = true; break;
                        }
                    }

                    if (!culled && isTouchingBoundary(b1PreciseBounds, facing)) {
                        BlockEntity neighborBE = be.getLevel().getBlockEntity(be.getBlockPos().relative(facing.toVanilla()));
                        if (neighborBE instanceof BETiles) {
                            List<RenderBox> neighborSolid = getBoxesFromBE((BETiles) neighborBE, false);
                            int ox = facing.toVanilla().getStepX();
                            int oy = facing.toVanilla().getStepY();
                            int oz = facing.toVanilla().getStepZ();
                            for (RenderBox box2 : neighborSolid) {
                                if (covers(b1PreciseBounds, box2, facing, ox, oy, oz)) {
                                    culled = true; break;
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

            for (int i = 0; i < transBoxes.size(); i++) {
                RenderBox box1 = transBoxes.get(i);
                List<RenderBox> singleList = Collections.singletonList(box1);

                for (Facing facing : Facing.VALUES) {
                    List<BakedQuad> temp = new ArrayList<>();
                    CreativeBakedBoxModel.compileBoxes(singleList, facing, net.minecraft.client.renderer.Sheets.translucentCullBlockSheet(), rand, true, temp);
                    if (temp.isEmpty()) continue;

                    float[] b1PreciseBounds = getQuadBounds(temp);
                    boolean culled = false;

                    for (int j = 0; j < transBoxes.size(); j++) {
                        if (i == j) continue;
                        if (covers(b1PreciseBounds, transBoxes.get(j), facing, 0, 0, 0)) {
                            culled = true; break;
                        }
                    }

                    if (!culled && isTouchingBoundary(b1PreciseBounds, facing)) {
                        BlockEntity neighborBE = be.getLevel().getBlockEntity(be.getBlockPos().relative(facing.toVanilla()));
                        if (neighborBE instanceof BETiles) {
                            List<RenderBox> neighborTrans = getBoxesFromBE((BETiles) neighborBE, true);
                            int ox = facing.toVanilla().getStepX();
                            int oy = facing.toVanilla().getStepY();
                            int oz = facing.toVanilla().getStepZ();

                            for (RenderBox box2 : neighborTrans) {
                                if (covers(b1PreciseBounds, box2, facing, ox, oy, oz)) {
                                    culled = true; break;
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
            return cache;
        } catch (Exception e) {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static float[] getQuadBounds(List<BakedQuad> quads) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int step = data.length / 4;
            for (int i = 0; i < 4; i++) {
                int offset = i * step;
                float x = Float.intBitsToFloat(data[offset]);
                float y = Float.intBitsToFloat(data[offset + 1]);
                float z = Float.intBitsToFloat(data[offset + 2]);

                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (y < minY) minY = y; if (y > maxY) maxY = y;
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
            }
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean covers(float[] b1, RenderBox b2, Facing face, int ox, int oy, int oz) {
        if (b1 == null) return false;
        float e = 0.001f;
        float b2minX = b2.minX + ox, b2maxX = b2.maxX + ox;
        float b2minY = b2.minY + oy, b2maxY = b2.maxY + oy;
        float b2minZ = b2.minZ + oz, b2maxZ = b2.maxZ + oz;

        switch (face) {
            case EAST:  return Math.abs(b1[3] - b2minX) < e && b2minY <= b1[1] + e && b2maxY >= b1[4] - e && b2minZ <= b1[2] + e && b2maxZ >= b1[5] - e;
            case WEST:  return Math.abs(b1[0] - b2maxX) < e && b2minY <= b1[1] + e && b2maxY >= b1[4] - e && b2minZ <= b1[2] + e && b2maxZ >= b1[5] - e;
            case UP:    return Math.abs(b1[4] - b2minY) < e && b2minX <= b1[0] + e && b2maxX >= b1[3] - e && b2minZ <= b1[2] + e && b2maxZ >= b1[5] - e;
            case DOWN:  return Math.abs(b1[1] - b2maxY) < e && b2minX <= b1[0] + e && b2maxX >= b1[3] - e && b2minZ <= b1[2] + e && b2maxZ >= b1[5] - e;
            case SOUTH: return Math.abs(b1[5] - b2minZ) < e && b2minX <= b1[0] + e && b2maxX >= b1[3] - e && b2minY <= b1[1] + e && b2maxY >= b1[4] - e;
            case NORTH: return Math.abs(b1[2] - b2maxZ) < e && b2minX <= b1[0] + e && b2maxX >= b1[3] - e && b2minY <= b1[1] + e && b2maxY >= b1[4] - e;
            default: return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isTouchingBoundary(float[] b1, Facing face) {
        float e = 0.001f;
        switch(face) {
            case EAST: return Math.abs(b1[3] - 1.0f) < e;
            case WEST: return Math.abs(b1[0] - 0.0f) < e;
            case UP: return Math.abs(b1[4] - 1.0f) < e;
            case DOWN: return Math.abs(b1[1] - 0.0f) < e;
            case SOUTH: return Math.abs(b1[5] - 1.0f) < e;
            case NORTH: return Math.abs(b1[2] - 0.0f) < e;
            default: return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static float getDirectionalShade(BakedQuad quad) {
        if (!quad.isShade()) return 1.0f;
        switch (quad.getDirection()) {
            case DOWN: return 0.5f;
            case NORTH:
            case SOUTH: return 0.8f;
            case WEST:
            case EAST: return 0.6f;
            case UP:
            default: return 1.0f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ColoredQuad {
        public BakedQuad quad;
        public float r, g, b;

        public ColoredQuad(BakedQuad quad, RenderBox box) {
            this.quad = quad;
            int c = box.color;

            if ((c == -1 || c == 0xFFFFFFFF) && quad.isTinted() && box.state != null) {
                int tint = Minecraft.getInstance().getBlockColors().getColor(box.state, null, null, quad.getTintIndex());
                if (tint != -1) {
                    c = tint | 0xFF000000;
                }
            }

            this.r = ((c >> 16) & 0xFF) / 255.0f;
            this.g = ((c >> 8) & 0xFF) / 255.0f;
            this.b = (c & 0xFF) / 255.0f;
        }
    }

    public static class LRCache {
        public java.util.List<ColoredQuad> solidQuads = new java.util.ArrayList<>();
        public java.util.List<ColoredQuad> translucentQuads = new java.util.ArrayList<>();
    }

    @OnlyIn(Dist.CLIENT)
    private List<RenderBox> getBoxesFromBE(BETiles be, boolean targetTranslucent) {
        List<RenderBox> boxes = new ArrayList<>();
        try {
            Iterable<?> groups = (Iterable<?>) be.getClass().getMethod("groups").invoke(be);
            Method getRenderingBox = null;
            for (Object parent : groups) {
                if (getRenderingBox == null) {
                    for (Method m : parent.getClass().getMethods()) {
                        if (m.getName().equals("getRenderingBox") && m.getParameterCount() == 3) {
                            getRenderingBox = m; break;
                        }
                    }
                }
                if (getRenderingBox != null) {
                    for (Object tile : (Iterable<?>) parent) {
                        boolean isTrans = false;
                        try { isTrans = (boolean) tile.getClass().getMethod("isTranslucent").invoke(tile); } catch (Exception e) {}
                        if (isTrans == targetTranslucent) {
                            RenderType layer = isTrans ? RenderType.translucent() : RenderType.cutout();
                            for (Object box : (Iterable<?>) tile) {
                                RenderBox renderBox = (RenderBox) getRenderingBox.invoke(parent, tile, box, layer);
                                if (renderBox != null) boxes.add(renderBox);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return boxes;
    }
}