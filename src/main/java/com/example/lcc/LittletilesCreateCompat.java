package com.example.lcc;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import team.creative.littletiles.LittleTilesRegistry;

@Mod(LittletilesCreateCompat.MOD_ID)
public class LittletilesCreateCompat {

    public static final String MOD_ID = "lcc";

    public LittletilesCreateCompat(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {

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