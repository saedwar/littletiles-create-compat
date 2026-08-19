package com.example.lcc;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.math.transformation.LittleBlockTransformer;

public class LittleTilesBETransformer implements MovedBlockTransformerRegistries.BlockEntityTransformer {

    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        if (!(be instanceof BETiles tilesBe)) return;

        Direction.Axis axis = transform.rotationAxis;
        Rotation mcRot = transform.rotation;

        if (axis == null || mcRot == null || mcRot == Rotation.NONE) {
            return;
        }

        int steps = 0;
        switch (mcRot) {
            case CLOCKWISE_90: steps = 3; break;
            case CLOCKWISE_180: steps = 2; break;
            case COUNTERCLOCKWISE_90: steps = 1; break;
            default: return;
        }

        team.creative.creativecore.common.util.math.transformation.Rotation ltRot = null;
        switch (axis) {
            case Y: ltRot = team.creative.creativecore.common.util.math.transformation.Rotation.Y_CLOCKWISE; break;
            case X: ltRot = team.creative.creativecore.common.util.math.transformation.Rotation.X_CLOCKWISE; break;
            case Z: ltRot = team.creative.creativecore.common.util.math.transformation.Rotation.Z_CLOCKWISE; break;
        }

        if (ltRot != null) {
            LittleBlockTransformer.rotate(tilesBe, ltRot, steps);
            tilesBe.updateTiles(false, true);
        }
    }
}