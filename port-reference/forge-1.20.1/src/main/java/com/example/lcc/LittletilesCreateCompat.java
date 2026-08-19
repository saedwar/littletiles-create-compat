package com.example.lcc;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import team.creative.littletiles.LittleTilesRegistry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

@Mod("lcc")
public class LittletilesCreateCompat {

    public LittletilesCreateCompat() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LittleTilesMovementBehaviour behaviour = new LittleTilesMovementBehaviour();
            MovementBehaviour.REGISTRY.register(LittleTilesRegistry.BLOCK_TILES.get(), behaviour);
            MovementBehaviour.REGISTRY.register(LittleTilesRegistry.BLOCK_TILES_TICKING.get(), behaviour);
            MovementBehaviour.REGISTRY.register(LittleTilesRegistry.BLOCK_TILES_RENDERED.get(), behaviour);
            MovementBehaviour.REGISTRY.register(LittleTilesRegistry.BLOCK_TILES_TICKING_RENDERED.get(), behaviour);
            MovementBehaviour.REGISTRY.register(LittleTilesRegistry.SIGNAL_CONVERTER.get(), behaviour);

            LittleTilesBETransformer transformer = new LittleTilesBETransformer();

            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(LittleTilesRegistry.BE_TILES_TYPE.get(), transformer);
            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(LittleTilesRegistry.BE_TILES_TYPE_RENDERED.get(), transformer);
            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(LittleTilesRegistry.BE_SIGNALCONVERTER_TYPE.get(), transformer);
        });
    }
}