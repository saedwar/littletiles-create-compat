package com.example.lcc;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import team.creative.littletiles.LittleTilesRegistry;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;

@Mod(LittletilesCreateCompat.MOD_ID)
public class LittletilesCreateCompat {

    public static final String MOD_ID = "lcc";

    public LittletilesCreateCompat(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register LittleTiles blocks as Create movement actors.
            LittleTilesMovementBehaviour behaviour =
                    new LittleTilesMovementBehaviour();

            MovementBehaviour.REGISTRY.register(
                    LittleTilesRegistry.BLOCK_TILES.value(),
                    behaviour
            );

            MovementBehaviour.REGISTRY.register(
                    LittleTilesRegistry.BLOCK_TILES_TICKING.value(),
                    behaviour
            );

            MovementBehaviour.REGISTRY.register(
                    LittleTilesRegistry.BLOCK_TILES_RENDERED.value(),
                    behaviour
            );

            MovementBehaviour.REGISTRY.register(
                    LittleTilesRegistry.BLOCK_TILES_TICKING_RENDERED.value(),
                    behaviour
            );

            MovementBehaviour.REGISTRY.register(
                    LittleTilesRegistry.SIGNAL_CONVERTER.value(),
                    behaviour
            );

            // Rotate LittleTiles data when Create transforms a contraption.
            LittleTilesBETransformer transformer =
                    new LittleTilesBETransformer();

            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                    LittleTilesRegistry.BE_TILES_TYPE.value(),
                    transformer
            );

            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                    LittleTilesRegistry.BE_TILES_TYPE_RENDERED.value(),
                    transformer
            );

            MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                    LittleTilesRegistry.BE_SIGNALCONVERTER_TYPE.value(),
                    transformer
            );
        });
    }
}