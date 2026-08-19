package com.example.lcc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.littletiles.common.block.mc.BlockTile;

@Mixin(BlockTile.class)
public class BlockTileCompatMixin {

    @Inject(method = "hidesNeighborFace", at = @At("HEAD"), cancellable = true, remap = false)
    private void lcc$neverCull(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true, remap = true)
    private void lcc$alwaysPropagateLight(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
    @Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true, remap = true)
    private void lcc$emptyOcclusion(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        cir.setReturnValue(Shapes.empty());
    }
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true, remap = true)
    private void lcc$neverSkipRender(BlockState state, BlockState neighborState, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}