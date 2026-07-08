package bili.dongsz.broadcastradio.registry;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.RadioBaseStationBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BroadcastRadio.MOD_ID);
    
    public static final RegistryObject<BlockEntityType<SimpleRadioBlockEntity>> SIMPLE_RADIO_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("simple_radio_block_entity", 
                    () -> BlockEntityType.Builder.of(SimpleRadioBlockEntity::new, 
                            ModBlocks.SIMPLE_RADIO_BLOCK.get()).build(null));
    
    public static final RegistryObject<BlockEntityType<RadioBaseStationBlockEntity>> RADIO_BASE_STATION_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("radio_base_station_block_entity", 
                    () -> BlockEntityType.Builder.of(RadioBaseStationBlockEntity::new, 
                            ModBlocks.RADIO_BASE_STATION_BLOCK.get()).build(null));

    public static final RegistryObject<BlockEntityType<SimpleSignalJammerBlockEntity>> SIMPLE_SIGNAL_JAMMER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("simple_signal_jammer_block_entity",
                    () -> BlockEntityType.Builder.of(SimpleSignalJammerBlockEntity::new,
                            ModBlocks.SIMPLE_SIGNAL_JAMMER_BLOCK.get()).build(null));
}