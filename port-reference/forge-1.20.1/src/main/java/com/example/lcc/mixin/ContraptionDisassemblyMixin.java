package com.example.lcc.mixin;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.common.block.mc.BlockTile;

@Mixin(Contraption.class)
public class ContraptionDisassemblyMixin {


    @Redirect(
            method = "addBlocksToWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
            )
    )
    private VoxelShape spoofLittleTilesCollision(BlockState instance, BlockGetter level, BlockPos pos) {
        if (instance.getBlock() instanceof BlockTile) {
            return Shapes.block();
        }

        return instance.getCollisionShape(level, pos);
    }
}